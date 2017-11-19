package beans;

public class Paging {
	private Integer count;
	private Integer total;
	private Integer page;
	private Integer pages;
	
	public Paging(Integer count, Integer total, Integer page, Integer pages) {
		this.count = count;
		this.total = total;
		this.page = page;
		this.pages = pages;
	}
	
	//  GETTERS
	public Integer getCount() {
		return count;
	}
	
	public Integer getTotal() {
		return total;
	}
	
	public Integer getPage() {
		return page;
	}
	
	public Integer getPages() {
		return pages;
	}
	
	//  SETTERS
	public void setCount(Integer count) {
		this.count = count;
	}
	
	public void setTotal(Integer total) {
		this.total = total;
	}
	
	public void setPage(Integer page) {
		this.page = page;
	}
	
	public void setPages(Integer pages) {
		this.pages = pages;
	}
	
	//  METHODES
	@Override
	public String toString() {
		return "Paging{" +
				"count=" + count +
				", total=" + total +
				", page=" + page +
				", pages=" + pages +
				'}';
	}
}
