package download;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;


/**
* @author Kit
* @version: 2019年5月21日 下午12:16:30
* 
*/
public class HttpDownloader extends Downloader{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public HttpDownloader(URL url, String outputFolder, int numConnections) {
		super(url, outputFolder, numConnections);
		download();
	}
	
	private void error() {
		System.out.println("ERROR");
		setState(ERROR);
		setStateP(STATUSES[mState]);
	}
	
	@Override
	public void run() {
		HttpURLConnection conn = null;
		try {
			System.out.println(mURL.toString());
			conn = (HttpURLConnection)mURL.openConnection();
			conn.setConnectTimeout(10000);			
			conn.connect();
			
            if (conn.getResponseCode() / 100 != 2) {
                error();
            }            
            int contentLength = conn.getContentLength();
            if (contentLength < 1) {
                error();
            }			
            if (mFileSize == -1) {
            	mFileSize = contentLength;
        		setFileSizeP(mFileSize);
            	stateChanged();
            	System.out.println("File size: " + mFileSize);
            }               
            if (mState == DOWNLOADING) {
            	//新建下载线程
            	if (mListDownloadThread.size() == 0)
            	{
            		if (mFileSize > MIN_DOWNLOAD_SIZE) {
		                //计算每个线程下载量
						int partSize = Math.round(((float)mFileSize / mNumConnections) / BLOCK_SIZE) * BLOCK_SIZE;
						System.out.println("Part size: " + partSize);						
						//为下载线程分配任务
						int startByte = 0;
						int endByte = partSize - 1;
						HttpDownloadThread aThread = new HttpDownloadThread(1, mURL, mOutputFolder + mFileName, startByte, endByte);
						mListDownloadThread.add(aThread);
						int i = 2;
						while (endByte < mFileSize) {
							startByte = endByte + 1;
							endByte += partSize;
							aThread = new HttpDownloadThread(i, mURL, mOutputFolder + mFileName, startByte, endByte);
							mListDownloadThread.add(aThread);
							++i;
						}
            		} else {
            			HttpDownloadThread aThread = new HttpDownloadThread(1, mURL, mOutputFolder +  mFileName, 0, mFileSize);
            			mListDownloadThread.add(aThread);
            		}
            	} else { //重启所有线程
            		for (int i=0; i<mListDownloadThread.size(); ++i) {
            			mListDownloadThread.get(i).download();
            		}
            	}				
				//等待所有线程完成
				for (int i = 0; i < mListDownloadThread.size(); ++i) {
					mListDownloadThread.get(i).waitFinish();
				}			
				//downloading状态下运行到此处说明下载完成
				if (mState == DOWNLOADING) {
					setState(COMPLETED);
					setStateP(STATUSES[mState]);
				}
            }
		} catch (Exception e) {
			e.printStackTrace();
			error();
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}
	
	/**
	 * http下载线程
	 */
	private class HttpDownloadThread extends DownloadThread {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * 新建下载线程
		 * @param threadID
		 * @param url
		 * @param outputFile
		 * @param startByte
		 * @param endByte
		 */
		public HttpDownloadThread(int threadID, URL url, String outputFile, int startByte, int endByte) {
			super(threadID, url, outputFile, startByte, endByte);
		}

		@Override
		public void run() {
			BufferedInputStream in = null;
			RandomAccessFile raf = null;			
			try {
				HttpURLConnection conn = (HttpURLConnection)mURL.openConnection();				
				//设置下载字节范围
				String byteRange = mStartByte + "-" + mEndByte;
				conn.setRequestProperty("Range", "bytes=" + byteRange);
				System.out.println(mThreadID + " bytes=" + byteRange);				
				conn.connect();		

	            if (conn.getResponseCode() / 100 != 2) {
	                error();
	            }				

				in = new BufferedInputStream(conn.getInputStream());				
				//RandomAccessFile移动指针到开始标记位置
				raf = new RandomAccessFile(mOutputFile, "rw");
				raf.seek(mStartByte);
				
				byte data[] = new byte[BUFFER_SIZE];
				int numRead;
				while((mState == DOWNLOADING) && ((numRead = in.read(data,0,BUFFER_SIZE)) != -1))
				{
					raf.write(data,0,numRead);
					//标记
					mStartByte += numRead;
					//修改已下载字节数
					downloaded(numRead);
				}				
				if (mState == DOWNLOADING) {//因为无法再读取而跳出循环说明线程工作完成
					mIsFinished = true;
				}
			} catch (IOException e) {
				error();
				e.printStackTrace();
			} finally {
				if (raf != null) {
					try {
						raf.close();
					} catch (IOException e) {}
				}
				
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {}
				}
			}			
			System.out.println("End thread " + mThreadID);
		}
	}
}
