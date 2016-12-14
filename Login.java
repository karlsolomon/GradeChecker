import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;  
import javax.mail.*;  
import javax.mail.internet.*;  

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

public class Login implements Runnable{
	static String gradePage;
	static String password;
	static String username;
	static String gmail;
	static String gmailPassword;
	static Long waitTime;
	static Map<String, String> grades;		// unique, Grade
	static Map<String, String> courseNames;	// unique, CourseName
	
	public void initialize(String username, String password, String gmail, String gmailPassword, Map<String, String> uniqueIDs, Long waitTime) {
		Login.username = username;
		Login.password = password;
		Login.gmail = gmail;
		Login.gmailPassword = gmailPassword;
		Login.waitTime = waitTime;
		courseNames = uniqueIDs;
		grades = new HashMap<String, String>();
		for(String unique : uniqueIDs.keySet()) {
			grades.put(unique, "");
		}
	}
	public void checkGrades() throws Exception {
	    try (final WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
	        HtmlPage page = webClient.getPage("https://utdirect.utexas.edu/apps/student/gradereport/");
	        final HtmlForm form = page.getFormByName("Login");
	        final HtmlTextInput userName = form.getInputByName("IDToken1");
	        final HtmlPasswordInput passWord = form.getInputByName("IDToken2");

			userName.setValueAttribute(username);
			passWord.setValueAttribute(password);

	       final HtmlSubmitInput loginButton = page.getElementByName("Login.Submit");
	       page = loginButton.click();
	       
	       gradePage = page.asXml();
	       List<Course> courses = new ArrayList<>();
	       for(String course : grades.keySet()) {
	    	   courses.add(new Course(course));
	       }
	       
	       Map<String, String> updatedGrades = new HashMap<>();	//courseName, Grade
	       
	       boolean hasChanged = false;
	       for(Course i : courses) {
	    	   String grade = i.getGrade();
	    	   if(!grade.equals(grades.get(i.uniqueID))) {
	    		   hasChanged = true;
	    		   updatedGrades.put(i.courseName, grade);
	    		   grades.put(i.uniqueID, grade);
	    	   }
	       }	       
	       if(hasChanged) {
	    	   Email.sendEmail(gradeToMessage(updatedGrades));
	       }
	    }
	}
	
	private String gradeToMessage(Map<String, String> updates) {
		StringBuilder message = new StringBuilder();
		for(String i : updates.keySet()) {
			message.append(i + ": " + updates.get(i) + '\n');
		}
		return message.toString();
	}
	
	class Course {
		Integer gradeLocation;
		Integer classNameLocation;
		String uniqueID;
		String grade;
		String courseName;
		
		public Course (String uniqueID) {
			this.uniqueID = uniqueID;
			classNameLocation = gradePage.indexOf(uniqueID);
			gradeLocation = classNameLocation + uniqueID.length() + 165;
			courseName = courseNames.get(uniqueID);
		}
		
		protected String getGrade() {
			grade = gradePage.substring(gradeLocation, gradeLocation+20).trim(); 
			return grade;
		}		
	}
	
	static class Email {
		 public static void sendEmail(String text) {

		 String to=gmail;//change accordingly

		//Get the session object
		  Properties props = new Properties();
		  props.put("mail.smtp.host", "smtp.gmail.com");
		  props.put("mail.smtp.socketFactory.port", "465");
		  props.put("mail.smtp.socketFactory.class",
		        	"javax.net.ssl.SSLSocketFactory");
		  props.put("mail.smtp.auth", "true");
		  props.put("mail.smtp.port", "465");
		 
		  Session session = Session.getDefaultInstance(props,
		   new javax.mail.Authenticator() {
		   protected PasswordAuthentication getPasswordAuthentication() {
		   return new PasswordAuthentication(to,gmailPassword);//change accordingly
		   }
		  });
		 
		//compose message
		  try {
		   MimeMessage message = new MimeMessage(session);
		   message.setFrom(new InternetAddress(to));//change accordingly
		   message.addRecipient(Message.RecipientType.TO,new InternetAddress(to));
		   message.setSubject("Grade Report Update");
		   message.setText(text);
		   
		   //send message
		   Transport.send(message);

		   System.out.println("message sent successfully");
		 
		  } catch (MessagingException e) {throw new RuntimeException(e);}
		 
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				checkGrades();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
}

