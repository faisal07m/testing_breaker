class Customer {
	private String customer_name;
	private int account_id;
	private String card_details;
	private int card_pin;
	private double balance;

	/**
	 * @return the customer_name
	 */
	public String getCustomer_name() {
		return customer_name;
	}

	/**
	 * @param customer_name
	 *            the customer_name to set
	 */
	public void setCustomer_name(String customer_name) {
		this.customer_name = customer_name;
	}

	/**
	 * @return the account_id
	 */
	public int getAccount_id() {
		return account_id;
	}

	/**
	 * @param account_id
	 *            the account_id to set
	 */
	public void setAccount_id(int account_id) {
		this.account_id = account_id;
	}

	/**
	 * @return the card_details
	 */
	public String getCard_details() {
		return card_details;
	}

	/**
	 * @param card_details
	 *            the card_details to set
	 */
	public void setCard_details(String card_details) {
		this.card_details = card_details;
	}

	/**
	 * @return the card_pin
	 */
	public int getCard_pin() {
		return card_pin;
	}

	/**
	 * @param card_pin
	 *            the card_pin to set
	 */
	public void setCard_pin(int card_pin) {
		this.card_pin = card_pin;
	}

	/**
	 * @return the balance
	 */
	public double getBalance() {
		return balance;
	}

	/**
	 * @param balance
	 *            the balance to set
	 */
	public void setBalance(double balance) {
		this.balance = balance;
	}
}
