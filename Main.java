import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application{
	String textFile = "Clients.txt";
	TextField gmail = new TextField();
	PasswordField password = new PasswordField();
	Label courseLabel = new Label();
	Label numClasses = new Label();
	Label uniqueID = new Label("UniqueIDs");
	Label className = new Label("Class Names");
	Slider numCourses = new Slider();
	Slider refreshTime = new Slider();
	Label refreshLabel = new Label();
	Button submit = new Button("Submit");
	String eidText;
	String eidPasswordText;
	String email;
	String emailPassword;
	Integer classes;
	Integer refresh = 2;
	Integer time;
	Integer hours;
	Integer minutes;
	Long lastTime;
	Map<String, String> uniqueIDs = new HashMap<>();	
	
	public static void main(String[] args) {
		launch(args);

	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		boolean hasGmail = ConfirmBox.display("Credentials", "This application requires the use of your Utexas and gmail account credentials. Do you have both a UTEID and gmail account?");
		if(!hasGmail) {
			primaryStage.close();
			System.exit(0);
		}
		Group utexas = new Group();
		StackPane utexasPane = new StackPane();
		utexasPane.setPrefSize(200, 200);
		VBox login = new VBox();
		login.setLayoutX(20);
		login.setLayoutY(20);
		login.setSpacing(10);
		TextField eid = new TextField();
		eid.setPromptText("EID");
		PasswordField eidPassword = new PasswordField();
		eidPassword.setPromptText("EID Password");
		Button eidSubmit = new Button("Submit");
		login.getChildren().addAll(eid, eidPassword, eidSubmit);
		utexasPane.getChildren().add(login);

		utexas.getChildren().add(utexasPane);
		Scene scene = new Scene(utexas);
		primaryStage.setScene(scene);
		primaryStage.setTitle("UT EID LOGIN");
		primaryStage.setMinWidth(270);
		
		primaryStage.show();
		eidSubmit.setOnAction(e -> {
			eidText = eid.getText();
			eidPasswordText = eidPassword.getText();
			if(eidExists(eidText, eidPasswordText)) {

				Map<String, String> uniqueIDs = getUniquesAndCourses();
				StringBuilder sb = new StringBuilder();
				sb.append("Courses:\n");
				for(String i : uniqueIDs.keySet()) {
					sb.append(i + " " + uniqueIDs.get(i) +"\n");
				}
				boolean clean = false;
				try {
					sb.append("Gmail: " + getGmail() + "\n");
					sb.append("Refresh Time: " + getDelay() + " hours\n");
					clean = ConfirmBox.display("Change Settings", "Would you like to change your settings? They are currently:\n" + sb.toString() + "\nIf you would like to change these settings or they are inaccurate click \"yes\"");
					
				} catch (NullPointerException e1) {
					clean = true;//lost user Data. 
				}				
				if(clean) {
					cleanFile();
					gmailAndClasses(primaryStage);
				} else {
					run(primaryStage);
				}
			} else {
				gmailAndClasses(primaryStage);
			}
		});
	}
	
	private void run(Stage primaryStage) {
		long hour = 1000*60*60;
		Login utGrades = new Login();
		Map<String, String> uniqueIDs = getUniquesAndCourses();
		
		Group root = new Group();
		StackPane pane = new StackPane();
		Label nextCheck = new Label();
		nextCheck.setText("Checking");
		pane.getChildren().add(nextCheck);
		root.getChildren().add(pane);
		Scene scene = new Scene(root);
		hours = 0;
		minutes = 0;
		time = refresh*60;
		primaryStage.setScene(scene);
		primaryStage.setTitle("Refresh Window");
		primaryStage.setMinHeight(150);
		primaryStage.setOnShowing(e -> {
			nextCheck.setText("Checking in:\n" + hours.toString() + " hours, " + minutes.toString() + " minutes");
		});
		primaryStage.show();
		Timer timer = new Timer();
		TimerTask updateSearchTime = new TimerTask() {
			
			@Override
			public void run() {
				time -= 1;
				if(refresh < 1) {
					refresh = 1;
				}
				if(time < 1) {
					time = refresh*60;
				}
				hours = time/60;
				minutes = time%60;
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						nextCheck.setText("Close this window to terminate the program.\n\nChecking in:\n" + hours.toString() + " hours, " + minutes.toString() + " minutes");
					}
				});
			}
		};
		timer.schedule(updateSearchTime, 0, 1000*60);
		
		primaryStage.setOnCloseRequest(e -> {
			boolean close = ConfirmBox.display("Are you Sure?", "Closing the refresh window will terminate the program. Are you sure you want to terminate the program?");
			if(close) {
				primaryStage.close();
				System.exit(0);
			} else {
				e.consume();
			}
		});
		
		try {
			utGrades.initialize(eidText, eidPasswordText, getGmail(), getGmailPassword(), uniqueIDs, refresh*hour);
			Thread check = new Thread(utGrades);
			check.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean eidExists(String eid, String password) {
		try{
			File clients = new File("Clients.txt");
			BufferedReader bf = new BufferedReader(new FileReader(clients.getAbsolutePath()));
			StringBuilder sb = new StringBuilder();
			String line;
			do {
				line = bf.readLine();
				sb.append(line+"\n");
			} while(line != null);
		    bf.close();
		    if(sb.toString().contains(eid)) {
		    	if (sb.toString().contains(password))
		    		return true;
		    	else {
		    		AlertBox.display("Incorrect Password", "Password changed to:" + password);
		    		cleanFile();
		    	}
		    }
		    else {
		    	cleanFile();
		    }
		} catch (IOException e) {
			cleanFile();
			
		}
		return false;
	}
	
	private void cleanFile() {
		PrintWriter writer;
		try {
			FileOutputStream file = new FileOutputStream(textFile);
			writer = new PrintWriter(file);
		    writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void gmailAndClasses(Stage primaryStage) {
		Group root = new Group();
		StackPane p = new StackPane();
		p.setPrefSize(300, 200);
		VBox fields = new VBox();
		fields.setLayoutX(20);
		fields.setLayoutY(20);
		fields.setSpacing(10);
		HBox data = new HBox();
		VBox uniques = new VBox();
		VBox courses = new VBox();
		uniques.getChildren().add(uniqueID);
		courses.getChildren().add(className);
		data.getChildren().addAll(courses,uniques);
		fields.getChildren().addAll(gmail,password,refreshLabel, refreshTime, courseLabel,numCourses, data, submit);
		p.getChildren().add(fields);
		gmail.setPromptText("youGmailHere@gmail");
		password.setPromptText("Gmail Password");
		courseLabel.setText("Number of Courses");
		numCourses.setMax(10);
		numCourses.setMin(1);
		refreshTime.setMax(12);
		refreshTime.setMin(1);
		refreshLabel.setText("Time between queries (hours): ");
		
		refreshTime.valueProperty().addListener((obs, oldval, newVal) -> {
			Integer value = newVal.intValue();
			refreshTime.setValue(value);
			refreshLabel.setText("Time between queries (hours): " + value.toString());
			refresh = value;
		});
		
		StackPane.setAlignment(fields,Pos.CENTER);
	
		root.getChildren().add(p);
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.setTitle("Grade Checker Portal");
		primaryStage.show();
		
		p.heightProperty().addListener(new ChangeListener<Number>() {
		    @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) {
		        primaryStage.setMinHeight(newSceneHeight.doubleValue());
		    }
		});
		
		numCourses.valueProperty().addListener((obs, oldval, newVal) -> {
			Integer value = newVal.intValue();
			numCourses.setValue(value);
			courseLabel.setText("Number of Courses: " + value.toString());
			Integer oldValue = uniques.getChildren().size() - 1;
			if(oldValue < value) {
				for(int i = oldValue; i < value; i++) {
					uniques.getChildren().add(new TextField("unique " + Integer.toString(i)));
					courses.getChildren().add(new TextField("class name " + Integer.toString(i)));
				}
			}
			else if (oldValue > value) {
				for(int i = oldValue; i > value; i--) {
					uniques.getChildren().remove(i);
					courses.getChildren().remove(i);
				}
			}
		});
		
		submit.setOnAction(e -> {
			System.out.println("clicked submit");
			email = gmail.getText();
			emailPassword = password.getText();
			populateData(uniques.getChildren(), courses.getChildren(), eidText, eidPasswordText, email, emailPassword, refresh);
			run(primaryStage);
		});
	}
	
	private void populateData(List<Node> uniques, List<Node> courses, String eid, String eidPassword, String gmail, String password, Integer timeDelay) {
		try{
			FileOutputStream file = new FileOutputStream(textFile,true);
			PrintWriter out = new PrintWriter(file);
			out.println("credentials," +eid + "," + eidPassword);
			out.println("email," + gmail + "," + password);
			for(int i = 0; i < uniques.size(); i++) {
				if(uniques.get(i) instanceof TextField)
					out.println("course," + ((TextField) uniques.get(i)).getText() + "," + ((TextField) courses.get(i)).getText());
			}
			out.println("delay,"+timeDelay.toString());
			out.close();
		} catch (IOException e) {
			    e.printStackTrace();
		}		
	}
	
	private Map<String, String> getUniquesAndCourses() {
		try {
			File clients = new File(textFile);
			BufferedReader bf = new BufferedReader(new FileReader(clients.getAbsolutePath()));
			String line;
			String[] courseInfo;
			Map<String, String> courses = new HashMap<>();

			while((line = bf.readLine()) != null){
				if(line.startsWith("course")) {
					courseInfo = line.split(",");
					courses.put(courseInfo[1], courseInfo[2]);				
				}
			}
			bf.close();
			return courses;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private String getDelay() {
		try {
			File clients = new File(textFile);
			BufferedReader bf = new BufferedReader(new FileReader(clients.getAbsolutePath()));
			String line;
			String[] delay;
			do {
				line = bf.readLine();
				if(line.startsWith("delay")) {
					delay = line.split(",");
					refresh = Integer.parseInt(delay[1]);
					bf.close();
					return delay[1];			
				}
			} while(line != null);
			bf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return "";
	}
	
	private String getGmail() {
		try {
			File clients = new File(textFile);
			BufferedReader bf = new BufferedReader(new FileReader(clients.getAbsolutePath()));
			String line;
			String[] emailInfo;
			do {
				line = bf.readLine();
				if(line.startsWith("email")) {
					emailInfo = line.split(",");
					bf.close();
					return emailInfo[1];			
				}
			} while(line != null);
			bf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return "";
	}
	
	
	private String getGmailPassword() {
		try {
			File clients = new File(textFile);
			BufferedReader bf = new BufferedReader(new FileReader(clients.getAbsolutePath()));
			String line;
			String[] emailInfo;
			do {
				line = bf.readLine();
				if(line.startsWith("email")) {
					emailInfo = line.split(",");
					bf.close();
					return emailInfo[2];			
				}
			} while(line != null);
			bf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return "";
	}

}
