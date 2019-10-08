package download;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Observable;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


/**
* @author Kit
* @version: 2019年5月21日 下午12:18:24
* 
*/
public abstract class Downloader extends Observable implements Runnable, Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** 下载链接 */
	protected URL mURL;

	/** 下载线程数 */
	protected int mNumConnections;
	
	/** 下载文件名 */
	protected String mFileName;
	
	/** 文件字节数大小 */
	protected int mFileSize;
	
	/** 保存路径 */
	protected String mOutputFolder;
	
	/** 已下载字节数大小 */
	protected int mDownloaded;
	
	/** 下载状态（下载中，暂停等） */
	protected int mState;
	
	//与UI绑定的属性
	protected transient StringProperty fileNameP = new SimpleStringProperty();
	protected transient StringProperty fileSizeP = new SimpleStringProperty();
	protected transient StringProperty outputFolderP = new SimpleStringProperty();
	protected transient StringProperty downloadedP = new SimpleStringProperty();
	protected transient StringProperty stateP = new SimpleStringProperty();
    
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
    	s.defaultReadObject();
    	fileNameP = new SimpleStringProperty(mFileName);
    	fileSizeP = new SimpleStringProperty(String.valueOf(mFileSize));
    	outputFolderP = new SimpleStringProperty(mOutputFolder);
    	downloadedP = new SimpleStringProperty(getProgress());
    	stateP = new SimpleStringProperty(STATUSES[mState]);   	
    }

    //设置property
	protected void setFileNameP(String fileName) {
		this.fileNameP.set(fileName);
	}
	protected void setFileSizeP(int fileSize) {
		this.fileSizeP.set(String.valueOf(fileSize));
	}
	protected void setOutputFolderP(String outputFolder) {
		this.outputFolderP.set(outputFolder);
	}
	protected void setDownloadedP(String downloaded) {
		this.downloadedP.set(downloaded);
		notifyObservers();
	}	
	protected void setStateP(String state) {
		this.stateP.set(state);
	}

	//UI获取属性的方法
	public StringProperty fileNamePProperty() {
		return fileNameP;
	}	
	public StringProperty fileSizePProperty() {
		return fileSizeP;
	}	
	public StringProperty outputFolderPProperty() {
		return outputFolderP;
	}	
	public StringProperty downloadedPProperty() {
		return downloadedP;
	}	
	public StringProperty statePProperty() {
		return stateP;
	}

	/** 保存下载线程的List */
	protected ArrayList<DownloadThread> mListDownloadThread;
	
	protected static final int BLOCK_SIZE = 4096;
	protected static final int BUFFER_SIZE = 4096;
	protected static final int MIN_DOWNLOAD_SIZE = BLOCK_SIZE * 100;
	
	// 下载状态
    public static final String STATUSES[] = {"Downloading",
    				"Paused", "Complete", "Cancelled", "Error"};
	
	// 下载状态
	public static final int DOWNLOADING = 0;
	public static final int PAUSED = 1;
	public static final int COMPLETED = 2;
	public static final int CANCELLED = 3;
	public static final int ERROR = 4;
	
	/**
	 * Downloader
	 * @param fileURL
	 * @param outputFolder
	 * @param numConnections
	 */
	protected Downloader(URL url, String outputFolder, int numConnections) {
		mURL = url;
		mNumConnections = numConnections;
		
		String fileURL = url.getFile();
		mFileName = fileURL.substring(fileURL.lastIndexOf('/') + 1);
		setFileNameP(mFileName);
		System.out.println("File name: " + mFileName);
		mFileSize = -1;
		setFileSizeP(mFileSize);
		mOutputFolder = outputFolder;
		setOutputFolderP(mOutputFolder);
		mDownloaded = 0;
		setDownloadedP(getProgress());
		mState = DOWNLOADING;
		setStateP(STATUSES[mState]);
		
		mListDownloadThread = new ArrayList<DownloadThread>();
	}
	
	/**
	 * 暂停 
	 */
	public void pause() {
		setState(PAUSED);
		setStateP(STATUSES[mState]);
		try {
			waitSync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 继续
	 */
	public void resume() {
		setState(DOWNLOADING);
		setStateP(STATUSES[mState]);
		download();
	}
	
	/**
	 * 取消
	 */
	public void cancel() {
		setState(CANCELLED);
		setStateP(STATUSES[mState]);
	}
	
	
	public String getURL() {
		return mURL.toString();
	}
	
	public int getFileSize() {
		return mFileSize;
	}
	
	public int getState() {
		return mState;
	}
	
	protected void setState(int value) {
		mState = value;
		stateChanged();
	}
	
	public boolean isDownloading() {
		return mState == DOWNLOADING;
	}
	
	/**
	 * 计算下载百分比
	 */
	public String getProgress() {
		return String.format("%.2f", ((float)mDownloaded / mFileSize) * 100) + "%";
	}
	
	/**
	 * 开始下载
	 */
	protected void download() {
		Thread t = new Thread(this);
		t.start();
	}
	
	/**
	 * 修改已下载字节数大小
	 */
	protected synchronized void downloaded(int value) {
		mDownloaded += value;
		setDownloadedP(getProgress());
		stateChanged();
	}
	
	/**
	 * 状态改变，通知UI
	 */
	protected void stateChanged() {
		setChanged();
		notifyObservers();
	}
	
	/**
	 * 任务暂停后等待数据同步
	 * @throws InterruptedException
	 */
	private void waitSync() throws InterruptedException {
		for (DownloadThread dT : mListDownloadThread) {
			dT.waitFinish();
		}
	}
	
	/**
	 * 下载线程
	 */
	protected abstract class DownloadThread implements Runnable,Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		protected int mThreadID;
		protected URL mURL;
		protected String mOutputFile;
		protected int mStartByte;
		protected int mEndByte;
		protected boolean mIsFinished;
		protected transient Thread mThread;
		
		public DownloadThread(int threadID, URL url, String outputFile, int startByte, int endByte) {
			mThreadID = threadID;
			mURL = url;
			mOutputFile = outputFile;
			mStartByte = startByte;
			mEndByte = endByte;
			mIsFinished = false;
			
			download();
		}
		
		/**
		 * 本线程下载任务是否完成
		 */
		public boolean isFinished() {
			return mIsFinished;
		}
		
		/**
		 * 开始下载
		 */
		public void download() {
			if (!mIsFinished) {
				mThread = new Thread(this);
				mThread.start();
			}
		}
		
		/**
		 * 等待其他线程
		 * @throws InterruptedException
		 */
		public void waitFinish() throws InterruptedException {
			if (!mIsFinished) {
				mThread.join();
			}
		}
	}
}
