

import java.io.Serializable;

public class TransactionDetails implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String EntityName;
	private Integer UserID;
	private String TransactionRefNo;
	private String AreaCode;
	private Double TransactionAmount;
	private String Description;
	
	public String getEntityName() {
		return EntityName;
	}
	public void setEntityName(String entityName) {
		EntityName = entityName;
	}
	public Integer getUserID() {
		return UserID;
	}
	public void setUserID(Integer userID) {
		UserID = userID;
	}
	public String getTransactionRefNo() {
		return TransactionRefNo;
	}
	public void setTransactionRefNo(String transactionRefNo) {
		TransactionRefNo = transactionRefNo;
	}
	public String getAreaCode() {
		return AreaCode;
	}
	public void setAreaCode(String areaCode) {
		AreaCode = areaCode;
	}
	public Double getTransactionAmount() {
		return TransactionAmount;
	}
	public void setTransactionAmount(Double transactionAmount) {
		TransactionAmount = transactionAmount;
	}
	public String getDescription() {
		return Description;
	}
	public void setDescription(String description) {
		Description = description;
	}
	
}
