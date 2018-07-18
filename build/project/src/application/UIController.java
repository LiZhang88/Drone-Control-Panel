package application;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class UIController {
	@FXML
	private MenuItem Menu_Close;
	@FXML
	private MenuItem Menu_Camera;
	@FXML
	private MenuItem Menu_Jperf;
	@FXML
	private MenuItem Menu_NetTest;
	@FXML
	private Button Cam_Btn;
	@FXML
	private Button Delay_Btn;
	@FXML
	private Button Net_Btn;
	@FXML
	private Button Jperf_Btn;
	@FXML
	private ImageView Left_Cam;
	@FXML
	private ImageView Right_Cam;
	@FXML
	private ImageView logo;
	@FXML
	private MenuBar Menu_Bar;
	@FXML
	private Label Net_Flow;
	@FXML
	private Label Ping_Num;
	@FXML
	private Label cam_info;
	@FXML
	private TextField Ip_Addr;
	@FXML
	private TextField J_add;
	@FXML
	private TextField J_trans;
	@FXML
	private TextField J_band;
	@FXML
	private TextField left_text;
	@FXML
	private TextField right_text;
	@FXML
	private TextArea Jperf_text;
	@FXML
	private LineChart<String,Number> J_chart;
	@FXML
	private LineChart<String,Number> net_chart;
	@FXML
	private LineChart<String,Number> ping_chart;
	
	private VideoCapture left_capture = new VideoCapture();
	private VideoCapture right_capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive = false;
	private boolean pingActive = false;
	private boolean netActive = false;
	private boolean JperfActive = false;
	private boolean pingChartActive = false;
	private boolean netChartActive =false;
	private ScheduledExecutorService timer;
	private ScheduledExecutorService ping_timer;
	private ScheduledExecutorService net_timer;
	private float[] J_array;
	private int cam_status=0;
	@FXML
	protected void On_Menu_Close(ActionEvent event){
		Platform.exit();
	}
	@FXML
	public void initialize(){
		
		if(this.logo!=null){
			Image img=new Image("UNTBar.png");
			this.logo.setImage(img);
		}
	}
	@FXML
	protected void Switch_Stage(ActionEvent event) throws IOException{
		Stage stage;
		Parent root;
		//release_close();
		if(event.getSource()==Menu_Camera){
			stage= (Stage)Menu_Bar.getScene().getWindow();
			root = FXMLLoader.load(getClass().getResource("UI.fxml"));
		}
		else if(event.getSource()==Menu_Jperf){
			stage= (Stage)Menu_Bar.getScene().getWindow();
			root = FXMLLoader.load(getClass().getResource("JPerf.fxml"));
		}
		else{
			stage= (Stage)Menu_Bar.getScene().getWindow();
			root = FXMLLoader.load(getClass().getResource("NetTest.fxml"));
		}
		
		Scene scene = new Scene(root);
	    stage.setScene(scene);
	    //stage.setFullScreen(true);
	    stage.show();
	}
	@FXML
	protected void startCam(ActionEvent event)
	{
		if (!this.cameraActive)
		{
			// start the video capture
			//this.right_capture.open("rtsp://admin:1111@192.168.43.211/onvif/profile6/media.smp");
			//this.left_capture.open("rtsp://admin:admin@192.168.43.210/avc");
			String left=this.left_text.getText();
			String right=this.right_text.getText();
			this.right_capture.open(right);
			this.left_capture.open(left);
			//this.left_capture.open(0);
			//this.right_capture.open(0);
			// is the video stream available?
			if (this.left_capture.isOpened()&&this.right_capture.isOpened())
			{
				this.cameraActive = true;
				
				// grab a frame every 33 ms (30 frames/sec)
				Runnable left_frameGrabber = new Runnable() {
					
					@Override
					public void run()
					{
						if(!Thread.currentThread().isInterrupted()){
							Image left_imageToShow = grabFrame(left_capture);
							if(cam_status!=2){
								Left_Cam.setImage(left_imageToShow);								
							}
						}else{
							return;
						}
					}
				};
				
				Runnable right_frameGrabber = new Runnable() {
					
					@Override
					public void run()
					{
						if(!Thread.currentThread().isInterrupted()){
						Image right_imageToShow = grabFrame(right_capture);
						if(cam_status!=1){
							Right_Cam.setImage(right_imageToShow);								
						}
						}else{
							return;
						}
					}
				};

				this.timer = Executors.newScheduledThreadPool(10);
				this.timer.scheduleAtFixedRate(left_frameGrabber, 0, 15, TimeUnit.MILLISECONDS);
				this.timer.scheduleAtFixedRate(right_frameGrabber, 0, 15, TimeUnit.MILLISECONDS);
				// update the button content
				this.Cam_Btn.setText("Stop Camera");
			}
			else
			{
				// log the error
				System.err.println("Impossible to open the camera connection...");
			}
		}
		else
		{
			// the camera is not active at this point
			release_close();
		}
	}

	protected Image grabFrame(VideoCapture capture) {
		Image imageToShow = null;
		Mat frame = new Mat();
		
		// check if the capture is open
		if (capture.isOpened())
		{
			try
			{
				// read the current frame
				capture.read(frame);
				
				// if the frame is not empty, process it
				if (!frame.empty())
				{
					// convert the image to gray scale
					Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGB2BGR);
					Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGB2BGR);
					// convert the Mat object (OpenCV) to Image (JavaFX)
					imageToShow = mat2Image(frame);
				}
				
			}
			catch (Exception e)
			{
				// log the error
				System.err.println("Exception during the image elaboration: " + e);
			}
		}
		
		return imageToShow;
	}

	private Image mat2Image(Mat frame) {
		MatOfByte buffer = new MatOfByte();
		// encode the frame in the buffer
		Imgcodecs.imencode(".bmp", frame, buffer);
		// build and return an Image created from the image encoded in the
		// buffer
		return new Image(new ByteArrayInputStream(buffer.toArray()));
	}
	private void release_close(){
		this.cameraActive = false;
		// update again the button content
		this.Cam_Btn.setText("Start Camera");
		
		// stop the timer
		try
		{
			if(!this.timer.isTerminated()){
				this.timer.shutdown();
				this.timer.awaitTermination(30, TimeUnit.MILLISECONDS);
			}
		}
		catch (InterruptedException e)
		{
			// log the exception
			System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
		}
		
		// release the camera
		if(this.left_capture.isOpened()){
			this.left_capture.release();
		}
		if(this.left_capture.isOpened()){
			this.right_capture.release();
		}
		// clean the frame
		this.Left_Cam.setImage(null);
		this.Right_Cam.setImage(null);
	}
	public static String runIpCommand(String command){
		try{
			Process p=Runtime.getRuntime().exec(command);
			BufferedReader inputStream=new BufferedReader(
					new InputStreamReader(p.getInputStream()));
			String s="";
			while((s=inputStream.readLine())!=null){
				int pos=s.indexOf("Average");
				if(pos!=-1){
					//return s.substring(pos+10);
					System.out.println(s.substring(pos+10));
					return s.substring(pos+10); 
				}
			}
			return null;
		}catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}
	public static String[] runNetCommand(String command){
		try{
			Process p=Runtime.getRuntime().exec(command);
			BufferedReader inputStream=new BufferedReader(
					new InputStreamReader(p.getInputStream()));
			String s="";
			while((s=inputStream.readLine())!=null){
				int pos=s.indexOf("Bytes");
				if(pos!=-1){
					String[] P=s.split(" +");
					return P;
				}
			}
			return null;
		}catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}
	public static String JperfCommand(String command,float[] array){
		try{
			int i=0;
			Process p=Runtime.getRuntime().exec(command);
			BufferedReader inputStream=new BufferedReader(
					new InputStreamReader(p.getInputStream()));
			BufferedReader errors = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String s="";
			String error="";
			String result="";
			while((s=inputStream.readLine())!=null){
				result=result + s +'\n';
				int pos=s.indexOf("Mbits");
				if(pos !=-1 && i<array.length){
					String[] P=s.split(" +");
					array[i]=Float.parseFloat(P[6]);
					i++;
				}
			}
			while((error=errors.readLine())!=null){
				result=result + error +'\n';
			}
			p.waitFor(5000, TimeUnit.MILLISECONDS);
				return result;
		}catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}
	@FXML
	protected void get_ping(ActionEvent event){
		if(!this.pingActive){
			this.pingActive=true;
			String ipAdd;
			if(this.Ip_Addr.getText()!=null){
				ipAdd=this.Ip_Addr.getText();
			}
			else
			{
				this.Ping_Num.setText("Wrong IP");
				return;
			}
			Axis<String> xAxis = this.ping_chart.getXAxis();
			Axis<Number> yAxis = this.ping_chart.getYAxis();
	        xAxis.setAutoRanging(true);
	        yAxis.setAutoRanging(true);
	        //populating the series with data
			Task<Void> ping_Grabber=new Task<Void>(){
				@Override
				protected Void call() throws Exception {
					int i=0;
					XYChart.Series<String,Number> ping_series = new XYChart.Series<String,Number>();
					Platform.runLater(new Runnable(){
			        	@Override public void run(){
			        				ping_chart.getData().clear();
			        				ping_chart.getData().add(ping_series);
			        	}
					});
					while((!Thread.currentThread().isInterrupted())&&(pingActive)){
						i++;
						String ping=null;
						ping=runIpCommand("ping" + " "+ipAdd+"");
						
						if(ping!=null){
							updateMessage(ping);
							//System.out.println(ping.substring(0, ping.length()-2));
					        ping_series.getData().add(new XYChart.Data<String,Number>(Integer.toString(i), Integer.parseInt(ping.substring(0, ping.length()-2))));
					        Platform.runLater(new Runnable(){
					        	@Override public void run(){
					        		if (!pingChartActive){
					        			pingChartActive=true;
					        			ping_chart.getData().get(0);
					        		}
					        	}
					        });		
						}
						else
						{
							updateMessage("something wrong");
						}
						Thread.sleep(500);
					}
					return null;
				}
			};
			this.Ping_Num.textProperty().bind(ping_Grabber.messageProperty());
			this.ping_timer=Executors.newSingleThreadScheduledExecutor();
			this.ping_timer.submit(ping_Grabber);
			this.Delay_Btn.setText("Stop Delay Test");
		}
		else
		{
			this.pingActive=false;
			this.pingChartActive=false;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			try {
				this.ping_timer.shutdown();
				this.ping_timer.awaitTermination(1000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				 //TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.Ping_Num.textProperty().unbind();
			this.Ping_Num.setText("ms");
			this.Delay_Btn.setText("Start Delay Test");
		}
	}
	
	@FXML
	protected void get_net(ActionEvent event){
		if(!this.netActive){
			this.netActive=true;
			Axis<String> xAxis = this.net_chart.getXAxis();
			Axis<Number> yAxis = this.net_chart.getYAxis();
	        xAxis.setAutoRanging(true);
	        yAxis.setAutoRanging(true);	
			Task<Void> net_Grabber=new Task<Void>(){
				long r_i=0;
				long r_j=0;
				long r=0;
				long s_i=0;
				long s_j=0;
				long s=0;
				@Override
				protected Void call() throws Exception {
					int i=0;
					XYChart.Series<String,Number> net_series_r = new XYChart.Series<String,Number>();
					XYChart.Series<String,Number> net_series_s = new XYChart.Series<String,Number>();
					net_series_r.setName("received");
					net_series_s.setName("sent");
					Platform.runLater(new Runnable(){
			        	@Override public void run(){
			        				net_chart.getData().clear();
			        				net_chart.getData().add(net_series_r);
			        				net_chart.getData().add(net_series_s);
			        	}
					});
					while(!Thread.currentThread().isInterrupted()&&(netActive)){
						i++;
						String[] ping=null;
						ping=runNetCommand("netstat" + " -e");
						if(ping!=null){
							this.s_j=Long.parseLong(ping[2]);
							this.s=this.s_j-this.s_i;
							this.r_j=Long.parseLong(ping[1]);
							this.r=this.r_j-this.r_i;
							String msg="received: " + this.r +" sent: "+ this.s;
							updateMessage(msg);
							if(this.s_i!=0){
							net_series_r.getData().add(new XYChart.Data<String,Number>(Integer.toString(i), this.r/1000));
							net_series_s.getData().add(new XYChart.Data<String,Number>(Integer.toString(i), this.s/1000));
							Platform.runLater(new Runnable(){
					        	@Override public void run(){
					        		if (!netChartActive){
					        			netChartActive=true;
					        			net_chart.getData().get(0);
					        			net_chart.getData().get(1);
					        		}
					        	}
					        });	
							}
							this.s_i=this.s_j;
							this.r_i=this.r_j;
						}
						else
						{
							updateMessage("something wrong");
						}
						Thread.sleep(1000);
					}
					return null;
				}
			};
			this.Net_Flow.textProperty().bind(net_Grabber.messageProperty());
			this.net_timer=Executors.newSingleThreadScheduledExecutor();
			this.net_timer.submit(net_Grabber);
			this.Net_Btn.setText("Stop Net Flow Test");
		}
		else
		{
			this.netActive=false;
			this.netChartActive=false;
			try {
				this.net_timer.shutdown();
				this.net_timer.awaitTermination(1000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				 //TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.Net_Flow.textProperty().unbind();
			this.Net_Flow.setText("N/A");
			this.Net_Btn.setText("Start Net Flow Test");
		}
	}
	@FXML
	protected void Jperf(ActionEvent event){
		if(!this.JperfActive){
			this.Jperf_Btn.setText("Proccess..");
			this.Jperf_Btn.setDisable(true);
			this.JperfActive=true;
			String ping=null;
			String command="C:\\iperf\\iperf3.exe -u";
			String add=this.J_add.getText();
			String band=this.J_band.getText();
			String times=this.J_trans.getText();
			this.J_array=new float[Integer.parseInt(times)];
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ping=JperfCommand(command+ " -c "+add+" -p 5001 " +"-b "+band+"M "+"-t "+ times,this.J_array);
				if(ping!=null){
					Jperf_text.setText(ping);
				}
				else
				{
					Jperf_text.setText("something wrong");
				}
			this.Jperf_Btn.setText("Start JPerf");
			this.JperfActive=false;
			this.Jperf_Btn.setDisable(false);	
			
			Axis<String> xAxis = this.J_chart.getXAxis();
			Axis<Number> yAxis = this.J_chart.getYAxis();
	        //xAxis.setLabel("second");
	        //yAxis.setLabel("widthband");
	        xAxis.setAutoRanging(true);
	        yAxis.setAutoRanging(true);
	        //this.J_chart.setTitle("Widthband Line Chart");
	        XYChart.Series<String,Number> series = new XYChart.Series<String,Number>();
	        series.setName("Data");
	        //populating the series with data
	        for(int i=0;i<this.J_array.length;i++){
	        	series.getData().add(new XYChart.Data<String,Number>(Integer.toString(i), this.J_array[i]));
	        }
	        J_chart.getData().clear();
	        J_chart.getData().add(series);
	        
		}
	}
	@FXML
	protected void LeftZoom(ActionEvent event){
		//release_close();
		if(this.cam_status!=1){
			this.Left_Cam.setFitWidth(1440);
			this.Left_Cam.setFitHeight(680);
			this.Right_Cam.setFitWidth(1);
			this.Right_Cam.setFitHeight(1);
			this.cam_status=1;
		}
		else
		{
			this.Left_Cam.setFitWidth(640);
			this.Left_Cam.setFitHeight(480);
			this.Right_Cam.setFitWidth(640);
			this.Right_Cam.setFitHeight(480);
			this.cam_status=0;
		}
		
	}
	@FXML
	protected void RightZoom(ActionEvent event){
		//release_close();
		if(this.cam_status!=2){
			this.Right_Cam.setFitWidth(1440);
			this.Right_Cam.setFitHeight(680);
			this.Left_Cam.setFitWidth(1);
			this.Left_Cam.setFitHeight(1);
			this.cam_status=2;
		}
		else
		{
			this.Left_Cam.setFitWidth(640);
			this.Left_Cam.setFitHeight(480);
			this.Right_Cam.setFitWidth(640);
			this.Right_Cam.setFitHeight(480);
			this.cam_status=0;
		}
		
	}
}
