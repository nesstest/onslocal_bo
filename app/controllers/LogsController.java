package controllers;

import play.*;
import play.data.Form;
import play.mvc.*;
import views.html.*;
import models.*;
import play.data.FormFactory;
import play.data.validation.ValidationError;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.inject.*;
import javax.persistence.EntityManager;

import services.LoadToTarget;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
/**
 * This controller contains an action to process the input file
 */
public class LogsController extends Controller {
	@Inject 
	FormFactory formFactory;
	
//	@Inject
//	JPAApi jpaApi;
    
 //   @Transactional
    public Result showLog(){
    	Form<Logs> logForm = formFactory.form(Logs.class);
    	Logs l1 = new Logs();
    	File lfile = Play.application().getFile("/logs/application.log");
    	String logtext = "";
    	try {
    	 StringBuilder fileContents = new StringBuilder((int)lfile.length());
    	    Scanner scanner = new Scanner(lfile);
    	    String lineSeparator = System.getProperty("line.separator");

    	    try {
    	        while(scanner.hasNextLine()) {
    	            fileContents.append(scanner.nextLine() + lineSeparator);
    	        }
    	        logtext =  fileContents.toString();
    	    } finally {
    	        scanner.close();
    	    }
    	}
    	catch (Exception e)
    	{e.printStackTrace();}
    	l1.setLogtext(logtext);        
    	return ok(views.html.logs.render(logForm.fill(l1)));    
    }
    
}
