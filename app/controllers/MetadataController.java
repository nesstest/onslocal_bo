package controllers;

import play.data.Form;
import play.mvc.*;
import models.*;
import play.data.FormFactory;

import javax.inject.*;
import javax.persistence.EntityManager;

import services.MetadataParser;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;

import play.twirl.api.Html;

/**
 * This controller contains an action to process the input file
 */
public class MetadataController extends Controller {
	@Inject 
	FormFactory formFactory;
	
	private String json;
    private String dsname;
    private Long dimdsid;
	
	@Inject
	JPAApi jpaApi;
	
	
   @Transactional
   public Result processform() {
   	
   Form<Metadata> metaForm = formFactory.form(Metadata.class).bindFromRequest();
   	if(metaForm.hasErrors()) {
   	   return badRequest(views.html.metadata.render(metaForm));
   	} else {
  		
   		Metadata met1 = metaForm.get();
     	json = met1.getJson();
    	dsname = met1.getResourceId();
    //	dimdsid = met1.getDimdsid();
    	MetadataParser mp = new MetadataParser(met1);
		EntityManager em = jpaApi.em();
        mp.runJPA(em);
				
   // return ok(dsname + " " + dimdsid + " " + json );
    //	 ValidationError e = new ValidationError("name", "dataset already exist",new ArrayList());
    //	 ArrayList<ValidationError> errors = new ArrayList<ValidationError>();
    //	 errors.add(e);
    //	 dsForm.errors().put("name",errors);
    // 	Result badRequest = badRequest(views.html.load.render(dsForm));
    //	return badRequest;
   // 	return ok (ds1.getStatus());
    
    return ok(views.html.message.render(("Dataset " + dsname + " loaded"), Html.apply("<p>Dataset id: " + dsname + "</p>")));
 }
    
    }
    
}
