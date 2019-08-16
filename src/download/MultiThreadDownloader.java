package download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
* @author Kit
* @version: 2019年5月21日 上午10:52:52
* 
*/
public class MultiThreadDownloader extends Application implements Observer{
	
	private DownloadManager downloadManager;
	private TextField linkText, dirText;
	private Button startBtn, openBtn;
	private Button pauseBtn, resumeBtn, cancelBtn, delBtn;
	private TableView<Downloader> table;
	private ObservableList<Downloader> downloaders;
	private static String PATH = "tasks";
	
	public static void main(String[] args) {
		launch(args);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void start(Stage primaryStage) throws Exception {
		
		downloadManager = DownloadManager.getInstance();
		try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(PATH))) {
			ArrayList<Downloader> downloaders = (ArrayList<Downloader>) inputStream.readObject();
			downloadManager.setDownloadList(downloaders);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Label linkLabel = new Label("下载链接：");
		linkText = new TextField();
		startBtn = new Button("开始下载");
		startBtn.setPrefWidth(90);
		HBox linkBox = new HBox(linkLabel, linkText, startBtn);
		linkBox.setSpacing(5);
		
		startBtn.setOnAction(e -> {
			if (linkText.getText().trim().equals("")) {
				Alert alert = new Alert(AlertType.WARNING);
				alert.setTitle("下载失败");
				alert.setHeaderText(null);
				alert.setContentText("请输入下载链接");
				alert.show();
			} else if (dirText.getText().trim().equals("")) {
				Alert alert = new Alert(AlertType.WARNING);
				alert.setTitle("下载失败");
				alert.setHeaderText(null);
				alert.setContentText("请选择下载路径");
				alert.show();
			} else if (null == DownloadManager.verifyURL(linkText.getText())) {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("下载失败");
				alert.setHeaderText(null);
				alert.setContentText("链接无效");
				alert.show();
			} else {
				try {
					URL verifiedURL = new URL(linkText.getText());
					String outputFolder = dirText.getText();
					Downloader aDownloader = downloadManager.createDownload(verifiedURL, outputFolder);
					downloaders.add(aDownloader);
					aDownloader.addObserver(MultiThreadDownloader.this);
				} catch (MalformedURLException e1) {
					e1.printStackTrace();
					System.out.println("url err");
				}
			}
		});
		
		Label dirLabel = new Label("保存位置：");
		dirText = new TextField();
		openBtn = new Button("选择文件夹");
		openBtn.setPrefWidth(90);
		HBox dirBox = new HBox(dirLabel, dirText, openBtn);
		dirBox.setSpacing(5);
		
		openBtn.setOnAction(e -> {
			DirectoryChooser directoryChooser = new DirectoryChooser();
			File file = directoryChooser.showDialog(primaryStage);
			if (null != file) {
				dirText.setText(file.getPath());
			}
		});
        
        TableColumn nameCol = new TableColumn("文件名");
        nameCol.setCellValueFactory(new PropertyValueFactory<Downloader, String>("fileNameP"));
        nameCol.setPrefWidth(150);
        nameCol.setResizable(false);
        nameCol.setSortable(false);
        
        TableColumn sizeCol = new TableColumn("大小");
        sizeCol.setCellValueFactory(new PropertyValueFactory<Downloader, String>("fileSizeP"));
        sizeCol.setPrefWidth(100);
        sizeCol.setResizable(false);
        sizeCol.setSortable(false);
        
        TableColumn pathCol = new TableColumn("下载路径");
        pathCol.setCellValueFactory(new PropertyValueFactory<Downloader, String>("outputFolderP"));
        pathCol.setPrefWidth(150);
        pathCol.setResizable(false);
        pathCol.setSortable(false);
        
        TableColumn perCol = new TableColumn("百分比");
        perCol.setCellValueFactory(new PropertyValueFactory<Downloader, String>("downloadedP"));
        perCol.setPrefWidth(100);
        perCol.setResizable(false);
        perCol.setSortable(false);
        
        TableColumn stateCol = new TableColumn("状态");
        stateCol.setCellValueFactory(new PropertyValueFactory<Downloader, String>("stateP"));
        stateCol.setPrefWidth(100);
        stateCol.setResizable(false);
        stateCol.setSortable(false);
        
        downloaders = FXCollections.observableArrayList(downloadManager.getDownloadList());
		table = new TableView<Downloader>();
        table.setEditable(false);
        table.setItems(downloaders);
		table.getColumns().addAll(nameCol, sizeCol, pathCol, perCol, stateCol);
		table.setPrefWidth(600);
		table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		table.getSelectionModel().selectedItemProperty().addListener(
				new ChangeListener<Downloader>() {

					@Override
					public void changed(ObservableValue<? extends Downloader> observable, Downloader oldValue,
							Downloader newValue) {
						if (null != newValue) {
							System.out.println("newv notnull" + newValue.outputFolderPProperty());
							updateButtons(newValue);
						} else {
							System.out.println("newv null");
							onBtnDisable(true);
						}
					}
				});
		
		pauseBtn = new Button("暂停下载");
		resumeBtn = new Button("继续下载");
		cancelBtn = new Button("取消任务");
		delBtn = new Button("删除任务");
		onBtnDisable(true);
		
		pauseBtn.setOnAction(e -> {
			Downloader recentDownloader = table.getSelectionModel().getSelectedItem();
			recentDownloader.pause();
			updateButtons(recentDownloader);
		});
		
		resumeBtn.setOnAction(e -> {
			Downloader recentDownloader = table.getSelectionModel().getSelectedItem();
			recentDownloader.resume();
			updateButtons(recentDownloader);
		});
		
		cancelBtn.setOnAction(e -> {
			Downloader recentDownloader = table.getSelectionModel().getSelectedItem();
			recentDownloader.cancel();
			updateButtons(recentDownloader);
		});
		
		delBtn.setOnAction(e -> {
			Downloader aDownloader = table.getSelectionModel().getSelectedItem();
			aDownloader.cancel();
			downloadManager.removeDownload(aDownloader);
			downloaders.remove(aDownloader);
		});
		
		HBox btnBox = new HBox(pauseBtn, resumeBtn, cancelBtn, delBtn);
		btnBox.setSpacing(10);
		
		VBox vBox = new VBox(linkBox, dirBox, table, btnBox);
		vBox.setSpacing(10);
		vBox.setStyle("-fx-background-color: #ffffff;"
				+ "-fx-border-color: #ffffff;"
				+ "-fx-border-width:5px;");
		
		Scene scene = new Scene(vBox);
		primaryStage.setScene(scene);
		primaryStage.setTitle("Downloader");
		primaryStage.setResizable(false);
		primaryStage.show();
		
		primaryStage.setOnCloseRequest(e -> {
			ArrayList<Downloader> downloaders = downloadManager.getDownloadList();
			for (Downloader d : downloaders) {
				if (d.isDownloading()) {
					d.pause();
				}
			}
			try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(PATH))) {
				outputStream.writeObject(downloaders);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		});
		
	}
	
	private void onBtnDisable(boolean state) {
		pauseBtn.setDisable(state);
		resumeBtn.setDisable(state);
		cancelBtn.setDisable(state);
		delBtn.setDisable(state);
	}

	@Override
	public void update(Observable o, Object arg) {
		table.refresh();
		Downloader downloader = table.getSelectionModel().getSelectedItem();
		if (null != downloader && downloader.equals(o)) {
			updateButtons(downloader);
		}
	}
	
    private void updateButtons(Downloader recentDownloader) {
        if (recentDownloader != null) {
            int state = recentDownloader.getState();
            switch (state) {
                case Downloader.DOWNLOADING:
                    onBtnDisable(true);
                    pauseBtn.setDisable(false);
                    cancelBtn.setDisable(false);
                    break;
                case Downloader.PAUSED:
                	onBtnDisable(true);
                	resumeBtn.setDisable(false);
                	cancelBtn.setDisable(false);
                    break;
                case Downloader.COMPLETED:
                	onBtnDisable(true);
                	delBtn.setDisable(false);
                	break;
                case Downloader.CANCELLED:
                    System.out.println(state);
                	onBtnDisable(true);
                	delBtn.setDisable(false);
                    System.out.println("del" + state);
                	break;
                case Downloader.ERROR:
                	onBtnDisable(true);
                	delBtn.setDisable(false);
                    break;
                default: // COMPLETE or CANCELLED
                	System.out.println("observer update err");
                	break;
            }
        } else {
            // No download is selected in table.
        	onBtnDisable(true);
        }
    }
		
}
