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

import services.MetadataParser;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
/**
 * This controller contains an action to process the input file
 */
public class EditController extends Controller {
	@Inject 
	FormFactory formFactory;
	
	private String task = "TA";
    private String dsname;
    private Long dimdsid;
	
	@Inject
	JPAApi jpaApi;
    
    @Transactional
    public Result processform() {
    	Form<Editor> editForm = formFactory.form(Editor.class);
    	Editor ed1 = editForm.bindFromRequest().get();
    
    	task = ed1.getTask();
    	dsname = ed1.getDsname();
    	dimdsid = ed1.getDimdsid();
    //	String fullPath = "C:\\Users\\Admin\\Documents\\ILCH\\" + filename;
 //   	MetadataParser mp = new MetadataParser(ed1);
		EntityManager em = jpaApi.em();
  //      mp.runJPA(em);
		
		
    return ok(dsname + " " + dimdsid + " " + task );
    //	 ValidationError e = new ValidationError("name", "dataset already exist",new ArrayList());
    //	 ArrayList<ValidationError> errors = new ArrayList<ValidationError>();
    //	 errors.add(e);
    //	 dsForm.errors().put("name",errors);
    // 	Result badRequest = badRequest(views.html.load.render(dsForm));
    //	return badRequest;
   // 	return ok (ds1.getStatus());
    }
    
}
