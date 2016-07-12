package controllers;

import play.*;
import play.data.Form;
import play.mvc.*;
import play.twirl.api.Html;
import models.*;
import play.data.FormFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

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
    	Form<Editor> editForm = formFactory.form(Editor.class).bindFromRequest();
    	TimeZone tz = TimeZone.getTimeZone("Europe/London");
    	TimeZone.setDefault(tz);
    	
   	if(editForm.hasErrors()) {
		EntityManager em = jpaApi.em();
    	List <DimensionalDataSet> dis = em.createQuery("SELECT d FROM DimensionalDataSet d WHERE d.status = '1-Staging-OK'" ,DimensionalDataSet.class).getResultList();
    	ArrayList<String> datasetList = new ArrayList<String>();
    	for (int i=0; i<dis.size(); i++)
    	{
    		datasetList.add(dis.get(i).getDataResourceBean().getDataResource());
    	}
    	Collections.sort(datasetList);
    	em.flush();
    	em.clear();
   	   return badRequest(views.html.edit.render(editForm,datasetList));
   	} else {
    	
    	Editor ed1 = editForm.get();
    
    	task = ed1.getTask();
    	dsname = ed1.getDsname();
		EntityManager em = jpaApi.em();
    	List <DataResource> dis = em.createQuery("SELECT d FROM DataResource d WHERE d.dataResource = :dsid",DataResource.class).setParameter("dsid", dsname).getResultList();
    	// Logger.info("size = " + dis.size());
    	DataResource drs = dis.get(0);
    	List <DimensionalDataSet> dimds = em.createQuery("SELECT d FROM DimensionalDataSet d WHERE d.dataResourceBean = :dsid",DimensionalDataSet.class).setParameter("dsid", drs).getResultList();
    	Logger.info("size2 = " + dimds.size());
    	dimdsid = dimds.get(0).getDimensionalDataSetId();
      	Logger.info("dimsd = " + dimdsid);
    	ed1.setDimdsid(dimdsid);
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


        lot.runJPA(em);
	
        return ok(views.html.message.render(("Dataset " + dsname + " loaded to target"), Html.apply("<p>Dataset id: " + dsname + "</p>")));
   	 }		
   // return ok(dsname + " " + dimdsid + " " + task );
    //	 ValidationError e = new ValidationError("name", "dataset already exist",new ArrayList());
    //	 ArrayList<ValidationError> errors = new ArrayList<ValidationError>();
    //	 errors.add(e);
    //	 dsForm.errors().put("name",errors);
    // 	Result badRequest = badRequest(views.html.load.render(dsForm));
    //	return badRequest;
   // 	return ok (ds1.getStatus());
    }
    
}
