import java.io.Serializable;

public class ClassHospital implements Serializable {

	private String DRGDefinition;
	private Integer ProviderId;
	private String ProviderName;
	private String ProviderStreetAddress;
	private String ProviderCity;
	private String ProviderState;
	private Integer ProviderZipCode;
	private String HospitalReferralRegionDescription;
	private Integer TotalDischarges;
	private Double AverageCoveredCharges;
	private Double AverageTotalPayments;
	private Double AverageMedicarePayments;
	
	public String getDRGDefinition() {
		return DRGDefinition;
	}
	public void setDRGDefinition(String dRGDefinition) {
		DRGDefinition = dRGDefinition;
	}
	public Integer getProviderId() {
		return ProviderId;
	}
	public void setProviderId(Integer providerId) {
		ProviderId = providerId;
	}
	public String getProviderName() {
		return ProviderName;
	}
	public void setProviderName(String providerName) {
		ProviderName = providerName;
	}
	public String getProviderStreetAddress() {
		return ProviderStreetAddress;
	}
	public void setProviderStreetAddress(String providerStreetAddress) {
		ProviderStreetAddress = providerStreetAddress;
	}
	public String getProviderCity() {
		return ProviderCity;
	}
	public void setProviderCity(String providerCity) {
		ProviderCity = providerCity;
	}
	public String getProviderState() {
		return ProviderState;
	}
	public void setProviderState(String providerState) {
		ProviderState = providerState;
	}
	public Integer getProviderZipCode() {
		return ProviderZipCode;
	}
	public void setProviderZipCode(Integer providerZipCode) {
		ProviderZipCode = providerZipCode;
	}
	public String getHospitalReferralRegionDescription() {
		return HospitalReferralRegionDescription;
	}
	public void setHospitalReferralRegionDescription(String hospitalReferralRegionDescription) {
		HospitalReferralRegionDescription = hospitalReferralRegionDescription;
	}
	public Integer getTotalDischarges() {
		return TotalDischarges;
	}
	public void setTotalDischarges(Integer totalDischarges) {
		TotalDischarges = totalDischarges;
	}
	public Double getAverageCoveredCharges() {
		return AverageCoveredCharges;
	}
	public void setAverageCoveredCharges(Double averageCoveredCharges) {
		AverageCoveredCharges = averageCoveredCharges;
	}
	public Double getAverageTotalPayments() {
		return AverageTotalPayments;
	}
	public void setAverageTotalPayments(Double averageTotalPayments) {
		AverageTotalPayments = averageTotalPayments;
	}
	public Double getAverageMedicarePayments() {
		return AverageMedicarePayments;
	}
	public void setAverageMedicarePayments(Double averageMedicarePayments) {
		AverageMedicarePayments = averageMedicarePayments;
	}
}

