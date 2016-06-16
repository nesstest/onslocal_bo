package controllers;

import play.*;
import play.data.Form;
import play.mvc.*;
import views.html.*;
import models.*;
import play.data.FormFactory;
import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.io.File;

import javax.inject.*;
import javax.persistence.EntityManager;

import services.InputCSVParser;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.twirl.api.Html;


/**
 * This controller contains an action to process the input file
 */
public class LoadController extends Controller {
	@Inject 
	FormFactory formFactory;
	
	@Inject
	JPAApi jpaApi;
	
    private String id;
    private String title;   

    private String filename;
    private String status;
    
    @Transactional
    public Result processform() {
   	 

    	
    Form<Dataset> dsForm = formFactory.form(Dataset.class).bindFromRequest();
    	if(dsForm.hasErrors()) {
    	   return badRequest(views.html.load.render(dsForm));
    	} else {
    		
   	    play.mvc.Http.MultipartFormData<File> body = request().body().asMultipartFormData();
    	    play.mvc.Http.MultipartFormData.FilePart<File> filename = body.getFile("filename");	
    		
    	Dataset form = dsForm.get();
    	  id = form.getId();    		 
          title = form.getTitle();
       //   filename = filename.getFilename();
          status   = form.getStatus();
          String fileName = filename.getFilename();
    	 // String fullPath = "C:\\Users\\Admin\\Documents\\ILCH\\" + filename;
       //	  String fullPath = "C:\\ILCH\\" + filename;
          java.io.File file = filename.getFile();
       	  InputCSVParser inputCSV = new InputCSVParser(form,file);
   		  EntityManager em = jpaApi.em();  
   		  DataResource drs = new DataResource();
   		  drs.setDataResource(id);
   		  drs.setTitle(title);   		
   		  DimensionalDataSet dds = new DimensionalDataSet();
   		  dds.setDataResourceBean(drs);
   		  dds.setTitle(title);
   		  em.persist(drs);
   		  em.persist(dds);   
       	  inputCSV.runJPA(em, dds);
      
         //return ok(id + " " + title + " " + filename + " " + form.getStatus());
         
         return ok(views.html.message.render((id + " " + form.getStatus()), Html.apply("<p>Dataset id: " + id + "<br/>Dataset title: " + title + "<br/>Filename: " + fileName + "<br/>Status: " + form.getStatus() + "</p>")));
      }
   }
}

