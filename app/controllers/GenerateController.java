package controllers;

import play.*;
import play.data.Form;
import play.mvc.*;
import play.twirl.api.Html;
import views.html.*;
import models.*;
import play.data.FormFactory;
import play.data.validation.ValidationError;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.*;
import javax.persistence.EntityManager;

import services.CSVGenerator;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
/**
 * This controller contains an action to process the input file
 */
public class GenerateController extends Controller {
	@Inject 
	FormFactory formFactory;
	
	private String task = "TA";
    private String dsname;
    private Long dimdsid;
	
	@Inject
	JPAApi jpaApi;
    
    @Transactional
    public Result processform() {
    	Form<Generate> genForm = formFactory.form(Generate.class).bindFromRequest();
    	
   	if(genForm.hasErrors()) {
		EntityManager em = jpaApi.em();
    	List <DimensionalDataSet> dis = em.createQuery("SELECT d FROM DimensionalDataSet d WHERE d.status IN ('2-Target-OK','3-Attributes-OK','4-Metadata-OK','5-Generate-Failed','5-Generate-OK')" ,DimensionalDataSet.class).getResultList();
    	ArrayList<String> datasetList = new ArrayList<String>();
    	for (int i=0; i<dis.size(); i++)
    	{
    		datasetList.add(dis.get(i).getDataResourceBean().getDataResource());
    	}
    	Collections.sort(datasetList);
    	em.flush();
    	em.clear();
   	   return badRequest(views.html.generate.render(genForm,datasetList));
   	} else {
    	
    	Generate g1 = genForm.get();
    
    //	task = gen1.getTask();
    	dsname = g1.getDsname();
		EntityManager em = jpaApi.em();
    	List <DataResource> dis = em.createQuery("SELECT d FROM DataResource d WHERE d.dataResource = :dsid",DataResource.class).setParameter("dsid", dsname).getResultList();
    	// Logger.info("size = " + dis.size());
    	DataResource drs = dis.get(0);
    	List <DimensionalDataSet> dimds = em.createQuery("SELECT d FROM DimensionalDataSet d WHERE d.dataResourceBean = :dsid",DimensionalDataSet.class).setParameter("dsid", drs).getResultList();
    	Logger.info("size2 = " + dimds.size());
    	dimdsid = dimds.get(0).getDimensionalDataSetId();
      	Logger.info("dimsd = " + dimdsid);
    	g1.setDimdsid(dimdsid);
    	CSVGenerator gen = new CSVGenerator(dsname);

        gen.runJPA(em,dimds.get(0));
	
        response().setContentType("application/x-download");  
        response().setHeader("Content-disposition","attachment; filename=" + dsname + ".csv"); 
		File file = Play.application().getFile("/logs/" + dsname + ".csv");
     //   return ok(new File("/logs/" + dsname + ".csv"));
		return ok(file);
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
