package controllers;

import play.data.Form;
import play.mvc.*;
import models.*;
import play.data.FormFactory;

import javax.inject.*;

import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
/**
 * This controller contains an action to process the input file
 */
public class EditAttribsController extends Controller {
	@Inject 
	FormFactory formFactory;
	
    private String dsid;
	
	@Inject
	JPAApi jpaApi;
    
    @Transactional
    public Result processform() {
    	Form<EditAttribs> editAttribsForm = formFactory.form(EditAttribs.class).bindFromRequest();
    	
   	if(editAttribsForm.hasErrors()) {
   	   return badRequest(views.html.editattribs.render(editAttribsForm));
   	} else {
    	
    	EditAttribs edat = editAttribsForm.get();
    
    	dsid = edat.getDsid();
   
       /* return ok(views.html.attribs.render(prepareAtts(dsid))); */
        return redirect(routes.AttributesController.prepareAtts(dsid));
   	 }		

    }
    
}
