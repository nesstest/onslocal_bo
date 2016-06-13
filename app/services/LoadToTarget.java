package services;
import javax.persistence.EntityManager;
import org.json.*;
import play.*;

import org.apache.commons.lang.StringUtils;

import utils.GeogHelper;
import utils.Utility;
import exceptions.CSVValidationException;
import exceptions.GLLoadException;
import models.*;

import java.util.Date;
import java.util.List;

import javax.inject.*;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;

// see http://guidance.data.gov.uk/dcat_fields.html
public class LoadToTarget implements Runnable {
	  private static final Logger.ALogger logger = Logger.of(LoadToTarget.class);
	EntityManager em;
	Long ddsid;
	String dsname;
	Boolean singlearea;
	Boolean singletime;
	Long recct;
	public LoadToTarget(Editor ed) {
		this.ddsid = ed.getDimdsid();
		this.dsname = ed.getDsname();
	}
	
	public void runJPA(EntityManager em) {
		this.em = em;
		run();
	}
	
	@Override
	public void run() {
		logger.info(String.format("Loading to Target started for dataset id " + ddsid + " (" +dsname+")"));
		DimensionalDataSet ds = em.find(DimensionalDataSet.class, ddsid);
		try {
			StageToTarget(ds);
			logger.info("loaded " + recct + " records");
			ds.setStatus("2-Target-OK");
			ds.setObscount(recct);
		//	this.dataset.setStatus("Loaded to staging");
			logger.info(String.format("Load to Target successful"));
		} catch (CSVValidationException validationException) {
	//		this.dataset.setStatus("Input file failed validation");
			ds.setStatus("2-Target-Failed");
			ds.setValidationMessage(validationException.getMessage());
			ds.setValidationException(validationException.getLocalizedMessage());
			logger.info(String.format("Loading to target not successful - " + validationException.getMessage() ));
		} catch (GLLoadException loadException) {
		//	this.dataset.setStatus("Loading of observations failed");
			ds.setStatus("2-Target-Failed");
			ds.setLoadException(loadException.getMessage());
			logger.info(String.format("Loading to target not successful - ",  loadException ));
		} finally {
			em.persist(ds);
		}
		

	}
	
	private void StageToTarget(DimensionalDataSet ds){
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
		CHANGE - count the times and areas and fix if only one
		CHANGE - Assume geography preloaded - error if not
		*/
		singlearea = true;
		singletime = true;
		try {
	//		DimensionalDataSet ds = em.find(DimensionalDataSet.class, ddsid);
			
			List areas =  em.createQuery("SELECT distinct s.geographicArea FROM StageDimensionalDataPoint s WHERE s.dimensionalDataSetId = " +ddsid, StageDimensionalDataPoint.class).getResultList();
			List times =  em.createQuery("SELECT distinct s.timePeriodCode FROM StageDimensionalDataPoint s WHERE s.dimensionalDataSetId = " +ddsid, StageDimensionalDataPoint.class).getResultList();
		 	logger.info("area count = " + areas.size());
		 	logger.info("time count =  " + times.size());
			if (areas.size() > 1){
				singlearea = false;
			}
			if (times.size() > 1){
				singletime = false;
			}
		 	
			// set default types
			UnitType ut = em.find(UnitType.class, "Persons");
			if (ut == null){
			   ut = new UnitType();
			   ut.setUnitType("Persons");
			   em.persist(ut);
			}
			ValueDomain vd = em.find(ValueDomain.class, "Count");
			if (vd == null){
			   vd = new ValueDomain();
			   vd.setValueDomain("Count");
			   em.persist(vd);
			}
			TimeType ttq = em.find(TimeType.class, "QUARTER");
			if (ttq == null){
			   ttq = new TimeType();
			   ttq.setTimeType("QUARTER");
			   em.persist(ttq);
			}
			TimeType ttm = em.find(TimeType.class, "MONTH");
			if (ttm == null){
			   ttm = new TimeType();
			   ttm.setTimeType("MONTH");
			   em.persist(ttm);
			}
			TimeType tty = em.find(TimeType.class, "YEAR");
			if (tty == null){
			   tty = new TimeType();
			   tty.setTimeType("YEAR");
			   em.persist(tty);
			}
			
			List results =  em.createQuery("SELECT s FROM StageDimensionalDataPoint s WHERE s.dimensionalDataSetId = " +ddsid, StageDimensionalDataPoint.class).getResultList();
			logger.info("records found = " + results.size());
			recct = 0L;
			
	    	String extCode = (String)areas.get(0);
			GeographicArea singlegeo = null;
			if (singlearea){
				List singleAreaList =  em.createQuery("SELECT a FROM GeographicArea a WHERE a.extCode = :ecode",GeographicArea.class).setParameter("ecode", extCode).getResultList();
				//		logger.info("arealist = " + areaList.size());
			
				if (singleAreaList.isEmpty()){
					logger.error(String.format("area " + extCode + " not found on database"));
					throw new CSVValidationException(String.format("area " + extCode + " not found on database"));
				}
				else
				{
					singlegeo = (GeographicArea)singleAreaList.get(0);
				}
			}
			TimePeriod singleTim = null;
			if (singletime){
				String timeCode = (String)times.get(0);
				List timeList =  em.createQuery("SELECT t FROM TimePeriod t WHERE t.name = :tcode",TimePeriod.class).setParameter("tcode", timeCode).getResultList();
				logger.info("timelist = " + timeList.size());
				if (timeList.isEmpty()){
					StageDimensionalDataPoint tsp = (StageDimensionalDataPoint)results.get(0);
					singleTim = new TimePeriod();
					singleTim.setName(timeCode);
					Date startDate = new Date();
					singleTim.setStartDate(startDate);
					singleTim.setEndDate(startDate);
					singleTim.setTimeTypeBean(ttq);
					if (tsp.getTimeType().equalsIgnoreCase("QUARTER"))
					{
						singleTim.setTimeTypeBean(ttq);
					}
					if (tsp.getTimeType().equalsIgnoreCase("MONTH"))
					{
						singleTim.setTimeTypeBean(ttm);
					}
					em.persist(singleTim);
				}
				else
				{
					singleTim = (TimePeriod)timeList.get(0);
				}
			}

	//		for (int i = 0; i < results.size(); i++){
			// just the first 100 for now
				for (int i = 0; i < 100; i++){
				StageDimensionalDataPoint sdp = (StageDimensionalDataPoint)results.get(i);
		    //	1. Create a skeleton dimensional data point record in memory
				DimensionalDataPoint dp = new DimensionalDataPoint();
				dp.setDimensionalDataSet(ds);
				dp.setValue(sdp.getValue());
				
		    //	2. Fetch the staged category records for current observation seq id
				List<StageCategory> clist = sdp.getStageCategories();
		
				StringBuffer variableText = new StringBuffer("");
		    //	3. For each staged category record
		    	for (int j = 0; j < clist.size(); j++){
	    	//	3.1. Try to fetch the concept id, if not found create new concept
		       		StageCategory scat = clist.get(j);
		    //		logger.info("catno = " + scat.getId().getDimensionNumber());
		    		String conceptName = scat.getConceptSystemLabelEng();
	    	//	3.2. Try to fetch the category id, if not found create new category
		    		String categoryName = scat.getCategoryNameEng();
		    		variableText.append(categoryName);
		    		if (j < clist.size()-1){
		    			variableText.append(" | ");
		    		}
		    	//	logger.info("catname = " + categoryName);

					List catList =  em.createQuery("SELECT c FROM Category c WHERE c.name = :cname",Category.class).setParameter("cname", categoryName).getResultList();
					Category cat = null;
			//		logger.info("catlist = " + catList.size());
					if (catList.isEmpty()){
		    		 cat = new Category();
			    	 cat.setName(categoryName);
		    		}
		    		else
		    		{
		    			cat = (Category)catList.get(0);
		    		}
	    		
		    		ConceptSystem consys = em.find(ConceptSystem.class, conceptName);
		    		if (consys == null){		
		    			consys = new ConceptSystem();
			    		consys.setConceptSystem(conceptName);
		    		}
		    		cat.setConceptSystemBean(consys);
		    		em.persist(cat);
//		    		consys.addCategory(cat);
		    		em.persist(consys);
				    //  	4. If no new items created in 3.1 and 3.2, fetch the variable id for the combo, else create a variable and a set of variablecategory records for it		    	
			    	
		    	}
		    	String variableName = variableText.toString();
				List varList =  em.createQuery("SELECT v FROM Variable v WHERE v.name = :vname",Variable.class).setParameter("vname", variableName).getResultList();
				Variable var = null;
			//	logger.info("varlist = " + varList.size());
				
				if (varList.isEmpty()){
	    		 var = new Variable();
		    	 var.setName(variableName);
		    	 var.setUnitTypeBean(ut);
		    	 var.setValueDomainBean(vd);
			     em.persist(var);
	    		}
	    		else
	    		{
	    			var = (Variable)varList.get(0);
	    		}

				
		   // 	5. Try to fetch the geographic area id by extcode, if not found create new geographic area and derive area and level types via lookup on first three digits of extcode
				GeographicArea geo = singlegeo;
				if (!singlearea){
				     extCode = sdp.getGeographicArea();
				     List areaList =  em.createQuery("SELECT a FROM GeographicArea a WHERE a.extCode = :ecode",GeographicArea.class).setParameter("ecode", extCode).getResultList();
					//		logger.info("arealist = " + areaList.size());
					if (areaList.isEmpty()){
						logger.error(String.format("area " + extCode + " not found on database"));
						throw new CSVValidationException(String.format("area " + extCode + " not found on database"));
					}
					else
					{
						geo = (GeographicArea)areaList.get(0);
					}
				}
		    	// 6. Try to fetch a time id for the current time code. If not found create a new time_period entry.
				TimePeriod tim = singleTim;
				if (!singletime){
					String timeCode = sdp.getTimePeriodCode();
					List timeList =  em.createQuery("SELECT t FROM TimePeriod t WHERE t.name = :tcode",TimePeriod.class).setParameter("tcode", timeCode).getResultList();

		//				logger.info("timelist2 = " + timeList.size());
				     int timecount = timeList.size();
					if (timeList.isEmpty()){
						tim = new TimePeriod();
						tim.setName(timeCode);
						Date startDate = new Date();
						tim.setStartDate(startDate);
						tim.setEndDate(startDate);
						tim.setTimeTypeBean(ttq);
						if (sdp.getTimeType().equalsIgnoreCase("QUARTER"))
						{
							tim.setTimeTypeBean(ttq);
						}
						if (sdp.getTimeType().equalsIgnoreCase("MONTH"))
						{
							tim.setTimeTypeBean(ttm);
						}
						em.persist(tim);
					}
					else
					{
						tim = (TimePeriod)timeList.get(0);
					}
				}

		    //	7. Try to find a population record for the current area / time combo, if not found create a new one for it
				PopulationPK ppk = new PopulationPK();
				ppk.setGeographicAreaId(geo.getGeographicAreaId());
				ppk.setTimePeriodId(tim.getTimePeriodId());
				Population pop = em.find(Population.class,ppk);
				if (pop == null){
					   pop = new Population();
				//	   pop.setId(ppk);
					   pop.setGeographicArea(geo);
					   pop.setTimePeriod(tim);
					   pop.setGeographicAreaExtCode(extCode);
					   em.persist(pop);
					}
		    //	8. we should now have all the required ids populated and can do a "persist" on the data.
				
				dp.setPopulation(pop);
				dp.setVariable(var);
				em.persist(dp);
				
				recct = recct + 1;

			}
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("Load to Target failed " + e.getMessage() ));
			throw new CSVValidationException(String.format("Load to Target failed " + e.getMessage()));

		}
	}

}
