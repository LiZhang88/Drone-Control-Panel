package application;
	
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.*;
import org.opencv.core.Core;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			BorderPane root = (BorderPane)FXMLLoader.load(getClass().getResource("UI.fxml"));
			Scene scene = new Scene(root,1440,900);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle("UNT Drone Control Panel");
			//primaryStage.setFullScreen(true);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		//loadLibrary();
		launch(args);
	}
	private static void loadLibrary() {
	    try {
	        InputStream in = null;
	        File fileOut = null;
	        String osName = System.getProperty("os.name");
	        //Utils.out.println(Main.class, osName);
	        if(osName.startsWith("Windows")){
	            int bitness = Integer.parseInt(System.getProperty("sun.arch.data.model"));
	            if(bitness == 32){
	        //        Utils.out.println(Main.class, "32 bit detected");
	                in = Main.class.getResourceAsStream("/opencv/x86/opencv_java310.dll");
	                fileOut = File.createTempFile("lib", ".dll");
	            }
	            else if (bitness == 64){
	                System.out.println("64 bit detected");
	                in = Main.class.getResourceAsStream("/opencv/x64/opencv_java310.dll");
	                fileOut = File.createTempFile("lib", ".dll");
	            }
	            else{
	        //        Utils.out.println(Main.class, "Unknown bit detected - trying with 32 bit");
	                in = Main.class.getResourceAsStream("/opencv/x86/opencv_java310.dll");
	                fileOut = File.createTempFile("lib", ".dll");
	            }
	        }
	        else if(osName.equals("Mac OS X")){
	            in = Main.class.getResourceAsStream("/opencv/mac/libopencv_java245.dylib");
	            fileOut = File.createTempFile("lib", ".dylib");
	        }


	        OutputStream out = FileUtils.openOutputStream(fileOut);
	        IOUtils.copy(in, out);
	        in.close();
	        out.close();
	        System.load(fileOut.toString());
	    } catch (Exception e) {
	        throw new RuntimeException("Failed to load opencv native library", e);
	    }
	}
}
