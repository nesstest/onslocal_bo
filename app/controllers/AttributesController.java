package controllers;

import play.*;
import play.data.Form;
import play.mvc.*;
import play.twirl.api.Html;
import utils.TimeHelper;
import models.*;
import play.data.FormFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.*;
import javax.persistence.EntityManager;

import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
/**
 * This controller contains an action to process the input file
 */
public class AttributesController extends Controller {
	@Inject 
	FormFactory formFactory;

	private String datasetid;
	private Long datasetdefid;
	
	@Inject
	JPAApi jpaApi;
	
	@Transactional
	public Result prepareAtts(String dsid) {
    	datasetid = dsid;
		EntityManager em = jpaApi.em();
		Form<Attribs> attForm = formFactory.form(Attribs.class);
    	Attribs a1 = new Attribs();
    	List <DataResource> dis = em.createQuery("SELECT d FROM DataResource d WHERE d.dataResource = :dsid",DataResource.class).setParameter("dsid", datasetid).getResultList();
    	Logger.info("datasetid = " + datasetid);
   // 	Logger.info("size = " + dis.size());
    	DataResource drs = dis.get(0);
    	a1.setColumn_concept(drs.getColumnConcept());
    	a1.setRow_concept(drs.getRowConcept());
    	a1.setDataResource(datasetid);
    	List <DimensionalDataSet> dimds = em.createQuery("SELECT d FROM DimensionalDataSet d WHERE d.dataResourceBean = :dsid",DimensionalDataSet.class).setParameter("dsid", drs).getResultList();
   // 	Logger.info("size2 = " + dimds.size());
    	datasetdefid = dimds.get(0).getDimensionalDataSetId();
    	ArrayList<String> conList = new ArrayList<String>();
    	List <ConceptSystem> cons = em.createQuery("SELECT c FROM ConceptSystem c",ConceptSystem.class).getResultList();
    
    //  Join not working - incomplete load?	
    //	List <Category> cons = em.createQuery("SELECT c FROM Category c WHERE EXISTS "
    //			+ "(SELECT dds.dimensionalDataSetId FROM DimensionalDataSet dds JOIN dds.dimensionalDataPoints ddp "
    //			+ "JOIN ddp.variable v JOIN v.categories cat WHERE ddp.variable.variableId = v.variableId "
    //			+ "AND cat.categoryId = c.categoryId "
    //			+ "AND dds.dataResourceBean=:dsid)",Category.class).setParameter("dsid", drs).getResultList();

    	conList.add("Area");
    	conList.add("Time");
    	for (int i=0; i< cons.size(); i++){
    //		conList.add(cons.get(i).getConceptSystemBean().getConceptSystem());
    		conList.add(cons.get(i).getConceptSystem());
    	}
    	a1.setConceptList(conList);
    	return ok(views.html.attribs.render(attForm.fill(a1),conList)); 
	}
	
	@Transactional
    public Result processform() {
    	EntityManager em = jpaApi.em();
		Form<Attribs> attForm = formFactory.form(Attribs.class);
    	Attribs a1 = attForm.bindFromRequest().get();
    	List <DimensionalDataSet> dimds = em.createQuery("SELECT d FROM DimensionalDataSet d WHERE d.dimensionalDataSetId = :dsid",DimensionalDataSet.class).setParameter("dsid", datasetdefid).getResultList();
    	DimensionalDataSet ds = dimds.get(0);
    	try {
			String timeStamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
			ds.setModified(timeStamp);
			ds.setValidationException("");
			ds.setLoadException("");
			ds.setValidationMessage("Success");
        	ds.setStatus("3-Attributes-OK");
    		List <DataResource> dis = em.createQuery("SELECT d FROM DataResource d WHERE d.dataResource = :dsid",DataResource.class).setParameter("dsid", datasetid).getResultList();
    		DataResource drs = dis.get(0);
    		drs.setRowConcept(a1.getRow_concept());
    		drs.setColumnConcept(a1.getColumn_concept());
    		em.merge(drs);
    	//	TimeHelper thelp = new TimeHelper();
    	//	List <TimePeriod> times = em.createQuery("SELECT t FROM TimePeriod t",TimePeriod.class).getResultList();
        //	for (int i=0; i< times.size(); i++){
        //			TimePeriod tt = times.get(i);
        //			String timeString = tt.getName();
        //			tt.setStartDate(thelp.getStartDate(timeString));
        //			tt.setEndDate(thelp.getEndDate(timeString));
        //			em.merge(tt);
        //	    	}
    	}
	    catch (Exception e) {
	    	e.printStackTrace();
	//    	logger.error(String.format("Edit Attributes Failed " + e.getMessage() ));
	    	ds.setStatus("3-Attributes-Failed");
	    	ds.setValidationException(String.format("Edit Attributes Failed " + e.getMessage()));
	}
	 finally {
		em.merge(ds);
		em.flush();
		em.clear();
	}
		return ok(views.html.message.render(("Attributes updated"), Html.apply("<p>Column Concept: " + a1.getColumn_concept() + "<br/>Row Concept: " + a1.getRow_concept() + "</p>")));

    }
    
}
