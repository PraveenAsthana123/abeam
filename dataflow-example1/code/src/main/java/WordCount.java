import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Distribution;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.Mean;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;

public class WordCount {

  static class ExtractRecordsFn extends DoFn<String, ClassHospital> {
	private static final long serialVersionUID = 1098768782L;
	private final Counter emptyLines = Metrics.counter(ExtractRecordsFn.class, "emptyLines");
    private final Distribution lineLenDist =
        Metrics.distribution(ExtractRecordsFn.class, "lineLenDistro");

    @ProcessElement
    public void processElement(@Element String element, OutputReceiver<ClassHospital> receiver) {
      lineLenDist.update(element.length());
      if (element.trim().isEmpty()) {
        emptyLines.inc();
      } else {
    	  try{
	    	  //System.out.println(element);
	    	  // Split the line into columns.
	    	  String[] tokens = element.split(ExampleUtils.TOKENIZER_CSV_COMMA, -1);
	    	  
	    	  // Output each word encountered into the output PCollection.
	    	  ClassHospital obj = new ClassHospital();
	    	  obj.setDRGDefinition(tokens[0]);
	    	  obj.setProviderId(Integer.parseInt(tokens[1]));
	    	  obj.setProviderName(tokens[2]);
	    	  obj.setProviderStreetAddress(tokens[3]);
	    	  obj.setProviderCity(tokens[4]);
	    	  obj.setProviderState(tokens[5]);
	    	  obj.setProviderZipCode(Integer.parseInt(tokens[6]));
	    	  obj.setHospitalReferralRegionDescription(tokens[7]);
	    	  obj.setTotalDischarges(Integer.parseInt(tokens[8]));
	    	  obj.setAverageCoveredCharges(Double.parseDouble(tokens[9]));
	    	  obj.setAverageTotalPayments(Double.parseDouble(tokens[10]));
	    	  obj.setAverageMedicarePayments(Double.parseDouble(tokens[11]));
	    	  receiver.output(obj);
    	  }catch(Throwable t) {
    		  t.printStackTrace();
    	  }
      }
    }
  }

  static class AggregateRecordsFn extends DoFn<ClassHospital, KV<String, Double>> {
	private static final long serialVersionUID = 8781739189L;
    private final Counter emptyLines = Metrics.counter(ExtractRecordsFn.class, "emptyLines");
    private final Distribution lineLenDist =
        Metrics.distribution(ExtractRecordsFn.class, "lineLenDistro");

    @ProcessElement
    public void processElement(@Element ClassHospital element, OutputReceiver<KV<String, Double>> receiver) {
      // Calculate the Average Covered Charges per state.
      receiver.output(KV.of(element.getProviderState(), element.getAverageCoveredCharges()));
    }
  }

  public static class FormatAsTextFn extends SimpleFunction<KV<String, Double>, String> {
	  @Override
	  public String apply(KV<String, Double> input) {
	    return input.getKey() + ": " + input.getValue();
	  }
  }

  public static class CalculateStateAverages
      extends PTransform<PCollection<String>, PCollection<KV<String, Double>>> {

	private static final long serialVersionUID = 345367678L;

	@Override
    public PCollection<KV<String, Double>> expand(PCollection<String> lines) {

      // Convert lines of text into individual records.
      PCollection<ClassHospital> records = lines.apply(ParDo.of(new ExtractRecordsFn()));

      // Aggregate all the Average Covered Charges for each state.
      PCollection<KV<String, Double>> stateAverages = records.apply(ParDo.of(new AggregateRecordsFn()));
      
      // Calculate the Average Covered Charges for each state.
      PCollection<KV<String, Double>> averageCoveredChargesPerState = stateAverages.apply(Mean.<String, Double>perKey());

      return averageCoveredChargesPerState;
    }
  }

  public interface StateAveragesOptions extends PipelineOptions {

    @Description("Path of the file to read from")
    @Default.String("gs://apache-beam-samples/shakespeare/kinglear.txt")
    String getInputFile();

    void setInputFile(String value);

    /** Set this required option to specify where to write the output. */
    @Description("Path of the file to write to")
    @Required
    String getOutput();

    void setOutput(String value);
  }

  static void runWordCount(StateAveragesOptions options) {
    Pipeline p = Pipeline.create(options);

    // Concepts #2 and #3: Our pipeline applies the composite CalculateStateAverages transform, and passes the
    // static FormatAsTextFn() to the ParDo transform.
    p.apply("ReadLines", TextIO.read().from(options.getInputFile()))
        .apply(new CalculateStateAverages())
        .apply(MapElements.via(new FormatAsTextFn()))
        .apply("WriteCounts", TextIO.write().to(options.getOutput()));

    p.run().waitUntilFinish();
  }

  public static void main(String[] args) {
	  StateAveragesOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(StateAveragesOptions.class);

    runWordCount(options);
  }
}

