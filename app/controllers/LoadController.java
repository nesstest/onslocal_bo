package controllers;

import play.*;
import play.data.Form;
import play.mvc.*;
import views.html.*;
import models.*;
import play.data.FormFactory;
import play.data.validation.ValidationError;

import java.util.ArrayList;

import javax.inject.*;
import javax.persistence.EntityManager;

import services.InputCSVParser;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;


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
       
    public Result processform() {
    	
    Form<Dataset> dsForm = formFactory.form(Dataset.class).bindFromRequest();
    	if(dsForm.hasErrors()) {
    	   return badRequest(views.html.load.render(dsForm));
    	} else {
    		
    	Dataset form = dsForm.get();
    	  id = form.getId();    		 
          title = form.getTitle();
          filename = form.getFilename();
          status   = form.getStatus();
    	  String fullPath = "C:\\Users\\Admin\\Documents\\ILCH\\" + filename;
       	  InputCSVParser inputCSV = new InputCSVParser(form,fullPath);
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
      
         return ok(id + " " + title + " " + filename + " " + form.getStatus());
      }
   }
}

