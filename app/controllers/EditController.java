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

import services.LoadToTarget;
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
    	
    	LoadToTarget lot = new LoadToTarget(ed1);
/*    	
For each staged dimensional data point matching the current dimensional data set id...
    	
    	1. Create a skeleton dimensional data point record in memory
    	2. Fetch the staged category records for current observation seq id
    	3. For each staged category record
    		3.1. Try to fetch the concept id, if not found create new concept
    		3.2. Try to fetch the category id, if not found create new category
    	4. If no new items created in 3.1 and 3.2, fetch the variable id for the combo, else create a variable and a set of variablecategory records for it
    	5. Try to fetch the geographic area id by extcode, if not found create new geographic area and derive area and level types via lookup on first three digits of extcode
    	6. Try to fetch a time id for the current time code. If not found create a new time_period entry.
    	7. Try to find a population record for the current area / time combo, if not found create a new one for it
    	8. we should now have all the required ids populated and can do a "persist" on the data.
*/

		EntityManager em = jpaApi.em();
        lot.runJPA(em);
	
		
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
