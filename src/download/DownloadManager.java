package download;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;


/**
* @author Kit
* @version: 2019年5月21日 下午12:16:40
* 
*/
public class DownloadManager {

	// 单例
	private volatile static DownloadManager sInstance = null;
	
	// 下载线程数
	private static final int DEFAULT_NUM_CONN_PER_DOWNLOAD = 4;
	// 默认存放路径
	public static final String DEFAULT_OUTPUT_FOLDER = "C:\\Users\\Administrator\\Desktop";

	private int mNumConnPerDownload;
	private ArrayList<Downloader> mDownloadList;
	
	/** Protected constructor */
	protected DownloadManager() {
		mNumConnPerDownload = DEFAULT_NUM_CONN_PER_DOWNLOAD;
		mDownloadList = new ArrayList<Downloader>();
	}
	
	public int getNumConnPerDownload() {
		return mNumConnPerDownload;
	}
	
	public void SetNumConnPerDownload(int value) {
		mNumConnPerDownload = value;
	}
	
	
	public Downloader getDownload(int index) {
		return mDownloadList.get(index);
	}
	
	public Downloader removeDownload(int index) {
		return mDownloadList.remove(index);
	}
	
	public boolean removeDownload(Downloader downloader) {
		return mDownloadList.remove(downloader);
	}
	
	public ArrayList<Downloader> getDownloadList() {
		return mDownloadList;
	}
	
	public void setDownloadList(ArrayList<Downloader> downloaders) {
		this.mDownloadList = downloaders;
	}
		
	public Downloader createDownload(URL verifiedURL, String outputFolder) {
		HttpDownloader fd = new HttpDownloader(verifiedURL, outputFolder + File.separator, mNumConnPerDownload);
		mDownloadList.add(fd);
		
		return fd;
	}
	
	/**
	 * 获取单例
	 * @return DownloadManager
	 */
	public static DownloadManager getInstance() {
		if (sInstance == null) {
			synchronized (Downloader.class) {
				if (sInstance == null) {
					sInstance = new DownloadManager();
				}	
			}
		}		
		return sInstance;
	}
	
	/**
	 * 检查下载链接合法性
	 * @param fileURL
	 * @return URL对象，null if 不合法
	 */
	public static URL verifyURL(String fileURL) {
		// Only allow HTTP URLs.
        if (!fileURL.toLowerCase().startsWith("http://"))
            return null;
        
        // Verify format of URL.
        URL verifiedUrl = null;
        try {
            verifiedUrl = new URL(fileURL);
        } catch (Exception e) {
            return null;
        }
        
        // Make sure URL specifies a file.
        if (verifiedUrl.getFile().length() < 2)
            return null;
        
        return verifiedUrl;
	}
}
