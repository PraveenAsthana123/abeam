import java.util.Arrays;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.common.collect.ImmutableList;

public class BigQueryReadIntegration {

	private static final Logger LOG = LoggerFactory.getLogger(BigQueryReadIntegration.class);
	private static PCollection<TransactionDetails> googleStoragerecords;
	private static String tableSpec = "transactions.transaction_details";
	private static TableSchema tableSchema = new TableSchema().setFields(
			ImmutableList.of(
					new TableFieldSchema().setName("ENTITY_NAME").setType("STRING").setMode("REQUIRED"),
					new TableFieldSchema().setName("ENTITY_ID").setType("NUMERIC").setMode("REQUIRED"),
					new TableFieldSchema().setName("TX_REF_NO").setType("STRING").setMode("REQUIRED"),
					new TableFieldSchema().setName("AREA_CODE").setType("STRING").setMode("REQUIRED"),
					new TableFieldSchema().setName("TX_AMOUNT").setType("FLOAT64").setMode("REQUIRED"),
					new TableFieldSchema().setName("DESCRIPTION").setType("STRING").setMode("REQUIRED")));

	public static void main(String[] args) {
		BigQueryIntegrationOptions options = PipelineOptionsFactory.fromArgs(args).withValidation()
				.as(BigQueryIntegrationOptions.class);

		runBigQueryIntegration(options);
	}

	public interface BigQueryIntegrationOptions extends PipelineOptions {

		@Description("Path of the file to read from")
		@Default.String("gs://apache-beam-samples/shakespeare/kinglear.txt")
		String getInputFile();

		void setInputFile(String value);
	}

	private static void runBigQueryIntegration(BigQueryIntegrationOptions options) {
		Pipeline p = Pipeline.create(options);
		
		PCollection<TableRow> records = 
				p.apply("ReadLines", TextIO.read()
						.from(options.getInputFile()))
				.apply(new ExtractRecords())
				.apply(MapElements.via(new FormatAsTableRowFn()));
		try{
			PCollection<TableRow> bigqueryRecords = readFromBigQuery(p);
			compareBigQueryAndGSBucketRecords(bigqueryRecords, googleStoragerecords);
		} catch (Throwable t) {
			LOG.error("ERROR: ", t);
		}
		p.run().waitUntilFinish();
	}

	private static PCollection<TableRow> readFromBigQuery(Pipeline p) {
		PCollection<TableRow> records = p.apply(
				BigQueryIO.readTableRows()
						  .from(tableSpec));
						  //.withSelectedFields(Arrays.asList(new String[] { "ENTITY_NAME", "ENTITY_ID" })));
		return records;
	}
	
	private static void compareBigQueryAndGSBucketRecords(PCollection<TableRow> bigqueryRecords, PCollection<TransactionDetails> googleStoragerecords) {
		// do comparison
		LOG.info("*******  BIG QUERY RECORDS  *******");
		LOG.info(bigqueryRecords.toString());
		LOG.info("******* GGL STORAGE RECORDS *******");
		LOG.info(googleStoragerecords.toString());
	}


	static class ExtractRecordsFn extends DoFn<String, TransactionDetails> {
		private static final long serialVersionUID = 1098768783L;

		@ProcessElement
		public void processElement(@Element String element, OutputReceiver<TransactionDetails> receiver) {
			if (!element.trim().isEmpty()) {
				try {
					// Split the line into columns.
					String[] tokens = element.split(ExampleUtils.TOKENIZER_CSV_COMMA, -1);

					// Output each word encountered into the output PCollection.
					TransactionDetails obj = new TransactionDetails();
					obj.setEntityName(tokens[0]);
					obj.setUserID(Integer.parseInt(tokens[1]));
					obj.setTransactionRefNo(tokens[2]);
					obj.setAreaCode(tokens[3]);
					obj.setTransactionAmount(Double.parseDouble(tokens[4]));
					obj.setDescription(tokens[5]);
					receiver.output(obj);
				} catch (Throwable t) {
					LOG.error("ERROR: ", t);
				}
			}
		}
	}

	public static class ExtractRecords extends PTransform<PCollection<String>, PCollection<TransactionDetails>> {

		private static final long serialVersionUID = 345367678L;

		@Override
		public PCollection<TransactionDetails> expand(PCollection<String> lines) {

			// Convert lines of text into individual records.
			googleStoragerecords = lines.apply(ParDo.of(new ExtractRecordsFn()));
			return googleStoragerecords;
		}
	}

	public static class FormatAsTableRowFn extends SimpleFunction<TransactionDetails, TableRow> {
		@Override
		public TableRow apply(TransactionDetails input) {
			return new TableRow()
						.set("ENTITY_NAME", input.getEntityName())
						.set("ENTITY_ID", input.getUserID())
						.set("TX_REF_NO", input.getTransactionRefNo())
						.set("AREA_CODE", input.getAreaCode())
						.set("TX_AMOUNT", input.getTransactionAmount())
						.set("DESCRIPTION", input.getDescription());
		}
	}

}

