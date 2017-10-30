package com.agen.wtp.translate;

/**
 * 
 * <label>
 *		wordtopdf实体适配器.
 * </label>
 * <p>
 *		<pre>
 *			wordtopdf后不管成功与否都会回掉Callback方法进行通知(如果有其它需求，可以扩展此类来实现)
 *		</pre>
 * </p>
 * @ClassName TranslateAdpter
 * @author TGJ  
 * @see BaseModel
 * @date 2017年10月25日 上午11:14:20 
 *    
 * @Copyright 2017 www.agen.com Inc. All rights reserved.
 */
public abstract class TranslateAdpter implements TranslateInterface { 
	
	private String fromPath;
	private String toPath;
	public String getToPath() {
		return toPath;
	}

	public void setToPath(String toPath) {
		this.toPath = toPath;
	}

	private String bgId;
	
	private Integer saveFormat = PDF;
	
	
	public Integer getSaveFormat() {
		return saveFormat;
	}

	public void setSaveFormat(Integer saveFormat) {
		this.saveFormat = saveFormat;
	}


	public String getFromPath() {
		return fromPath;
	}

	public void setFromPath(String fromPath) {
		this.fromPath = fromPath;
	}

	public String getBgId() {
		return bgId;
	}

	public void setBgId(String bgId) {
		this.bgId = bgId;
	}
	
	public enum TranslateStatus {
		SUCCESS,
		FAIL,
		RETRY
	}
	
	@Override
	public String toString() {
		return "TranslateAdpter [fromPath=" + fromPath + ", toPath=" + toPath + ", bgId=" + bgId + ", saveFormat="
				+ saveFormat + "]";
	}

}
