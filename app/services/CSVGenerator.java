package services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.EntityManager;
import javax.persistence.SqlResultSetMapping;

import play.*;

import org.apache.commons.lang.StringUtils;
import utils.Utility;
import exceptions.CSVValidationException;
import exceptions.GLLoadException;
import models.*;
import javax.inject.*;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;

/**
 * The Class CSVGenerator.
 */
public class CSVGenerator implements Runnable {
	public static final byte[] UTF8_BOM = new byte[] { -17, -69, -65 }; // Byte order marker helpful to Excel
	/** The _logger. */
    private static final Logger.ALogger logger = Logger.of(InputCSVParser.class);
	protected static final String COPYRIGHT = "(C) Crown Copyright";
	protected static final String DOUBLE_QUOTE = "\"";
	protected static final Character COMMA = ',';
	protected static final String GEOGRAPHIC_ID = "Geographic ID";
	protected static final String GEOGRAPHIC_AREA = "Geographic Area";
	protected static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
	protected CsvWriter output;
//	protected StartDetails startDetails;
//	protected Language language;
//	protected GeogObservations<String> geogObservations;
	protected Map<Long, Integer> offsetForDimItemSetId = new HashMap<Long, Integer>();
//	protected WdaObservationType obsType;
	protected boolean isGeogSig = true;
	protected String timeDimensionTitle = null;
	EntityManager em;
	DimensionalDataSet dds;
	String outFile;
	private static String VARIABLE_VALUE_QUERY = "SELECT ga.geographic_area_id, ga.ext_code, ga.name AS area_name, ga.geographic_level_type, ti.time_period_id, ti.name AS time_name, v.variable_id, v.value_domain," +
			 " v.unit_type, v.name AS variable_name, d.value" +
			 " FROM onslocal_data_bo.geographic_area ga, onslocal_data_bo.time_period ti, onslocal_data_bo.dimensional_data_point d, onslocal_data_bo.variable v" +
			 " WHERE ga.geographic_area_id = d.geographic_area_id and d.variable_id = v.variable_id and d.time_period_id = ti.time_period_id" +
			 " AND d.dimensional_data_set_id = ?1 ORDER BY ga.geographic_area_id, time_name, v.variable_id";
	/**
	 * Instantiates a new cSV dataset formatter.
	 * 
	 * @param output the output
	 * @param language the language
	 */

	public CSVGenerator(String outfile)  {
		this.outFile = outfile;
	}
	
	public void runJPA(EntityManager em, DimensionalDataSet dds) {
		this.em = em;
		this.dds = dds;
		run();
	}
	
	public void run() {
		logger.info(String.format("CSV Generation started for DDS Id: %s.", dds.getDimensionalDataSetId()));
		try {
			buildCSV();
		//	this.dataset.setStatus("Loaded to staging");
			dds.setStatus("5-Generate-OK");
			logger.info(String.format("CSV Generation completed successfully for DDS Id: %s.", dds.getDimensionalDataSetId()));
		} catch (CSVValidationException validationException) {
			// Update status to Observations loading failed
		//	this.dataset.setStatus("Input file failed validation");
			dds.setStatus("5-Generate-Failed");
			dds.setValidationMessage(validationException.getMessage());
			dds.setValidationException(validationException.getLocalizedMessage());
			logger.info(String.format("CSV Generation failed for DDS Id: %s : %s", dds.getDimensionalDataSetId(), validationException ));
		} catch (GLLoadException loadException) {
			// Update status to Observations loaded
		//	this.dataset.setStatus("Loading of observations failed");
			dds.setStatus("5-Generate-Failed");
			dds.setLoadException(loadException.getMessage());
			logger.info(String.format("CSV Generation failed for DDS Id: %s : %s", dds.getDimensionalDataSetId(), loadException ));
		} finally {
			em.persist(dds);
		}
	}
	
	
	public void buildCSV(){
		try {
		File file = new File("csvfiles/" + outFile + ".csv");
		FileOutputStream out = new FileOutputStream(file);
		out.write(UTF8_BOM);
		this.output = new CsvWriter(new PrintWriter(new OutputStreamWriter(out,"UTF-8"))); 
		List<DataDTO> results = getDataRecords(dds.getDimensionalDataSetId());
		logger.info("records found = " + results.size());
		writeHeader(results);
		writeData(results);
		output.endRow();
		output.outputField(COPYRIGHT);
		output.endRow();
		output.close();
		} catch (IOException e) {
		logger.error("Failed to create the CSV: ", e);
		throw new GLLoadException("Failed to write the output CSV: ", e);
	} catch (CSVValidationException ve) {
		throw ve;
	} catch (Exception e) {
		logger.error("Failed to save CSV file: ", e);
		throw new GLLoadException("Failed to save CSV file due to " + e.getMessage(), e);
		}
	}
	
	public void writeHeader(List<DataDTO> results) {
		output.outputField("Area Code");
		output.outputField("Area Name");
		output.outputField("Area Type");
		output.outputField("Time Period");
		Boolean samearea = true;
		Boolean sametime = true;
		String areaCode = "START";
		String timeCode = "START";
		int i = 0;
		while (samearea && sametime ){
			DataDTO curData = (DataDTO)(results.get(i));
			String heading = curData.getVariableName();
		//	logger.info("heading = " + heading);
			output.outputField(heading);
			String nextAreaCode = curData.getExtCode();
	//		logger.info("areaCode = " + nextAreaCode);
			String nextTimeCode = curData.getTimeName();
		//	logger.info("timeCode = " + nextTimeCode);
			if (areaCode.equals("START")){
				areaCode = nextAreaCode;
			}
			if (timeCode.equals("START")){
				timeCode = nextTimeCode;
			}
			if (!areaCode.equals(nextAreaCode)){
				samearea = false;
			}
			if (!timeCode.equals(nextTimeCode)){
				sametime = false;
			}
			i++;
		}
		output.endRow();
	}
	
	public void writeData(List<DataDTO> results) {
		int i=0;
		while ( i < results.size() ){
			DataDTO curData = (DataDTO)(results.get(i));
			String areaCode = curData.getExtCode();
			String areaName = curData.getAreaName();
			String areaType = curData.getGeographicLevelType();
			String timeCode = curData.getTimeName();
			output.outputField(areaCode);
			output.outputField(areaName);
			output.outputField(areaType);
			output.outputField(timeCode);
			Boolean samearea = true;
			Boolean sametime = true;
			while (sametime & samearea & i < results.size() )
			{
				BigDecimal value = curData.getValue();
				String sValue =  String.valueOf(value);
				output.outputField(sValue);
				i++;
				if (i == results.size())
				{
					sametime = false;
				}
				else
				{
					curData = (DataDTO)(results.get(i));
					String nextAreaCode = curData.getExtCode();
					String nextTimeCode = curData.getTimeName();
			//		logger.info("timeCode = " + nextTimeCode);
					if (!areaCode.equals(nextAreaCode)){
						samearea = false;
					}
					if (!timeCode.equals(nextTimeCode)){
						sametime = false;
					}
				}
			}
			output.endRow();

		}
		output.endRow();
	}

	
	
	public List<DataDTO> getDataRecords(Long ddsId)
	{
		
		@SuppressWarnings("unchecked")
		List<DataDTO> results = (List<DataDTO>) em
				.createNativeQuery(VARIABLE_VALUE_QUERY, "DataTableResult")
				.setParameter(1, new Long(ddsId)).getResultList();
		
		return results;
	}
	
	/**
	@Override
	public void start(StartDetails details) throws Exception {
		// Save start details
		//
		this.startDetails = details;
		WdaDatasetDefintion wdaDatasetDefintion = details.getWdaDatasetDefintion();
		WdaObservation observationForObsType = details.getWdaObservationDAO().getObservationForObsType(wdaDatasetDefintion.getDatasetDefinitionId());
		if (null != observationForObsType) {
			this.obsType = observationForObsType.getObsType();
		}
		// Output file header
		WdaDatasetCollection wdaDatasetCollection = wdaDatasetDefintion.getWdaDatasetCollection();
		output.outputField(wdaDatasetDefintion.getDatasetid());
		output.endRow();
		output.outputField(language.choose(wdaDatasetCollection.getCollectionTitle(), wdaDatasetCollection.getWelshCollectionTitle()));
		output.endRow();
		output.outputField(wdaDatasetDefintion.getDatasetDifferentiator() == null ? "" : wdaDatasetDefintion.getDatasetDifferentiator());
		output.endRow();
		output.outputField(wdaDatasetDefintion.getPublicationDate() == null ? "" : dateFormat.format(wdaDatasetDefintion.getPublicationDate()));
		output.endRow();
		output.endRow();
		// Get title details
		//
		List<String> measureTypes = new ArrayList<String>();
		List<String> statisticalUnits = new ArrayList<String>();
		List<String> dimTitles = new ArrayList<String>();
		List<String> dimItemTitles = new ArrayList<String>();
		determineTitles(measureTypes, statisticalUnits, dimTitles, dimItemTitles);
		// Output the measure types
		//
		if (!isGeogSig) {
			output.outputField("");
		}
		output.outputField("");
		output.outputField("");
		for (String measureType : measureTypes) {
			output.outputField(measureType);
		}
		output.endRow();
		// Output the statistical units
		//
		if (!isGeogSig) {
			output.outputField("");
		}
		output.outputField("");
		output.outputField("");
		for (String statisticalUnit : statisticalUnits) {
			output.outputField(statisticalUnit);
		}
		output.endRow();
		// Output the dimension titles
		//
		if (!isGeogSig) {
			output.outputField("");
		}
		output.outputField("");
		output.outputField("");
		for (String dimTitle : dimTitles) {
			output.outputField(dimTitle);
		}
		output.endRow();
		// Output the dimension item titles
		//
		if (!isGeogSig) {
			printTimeDimensionTitle(wdaDatasetDefintion);
		}
		output.outputField(language.choose(GEOGRAPHIC_ID));
		output.outputField(language.choose(GEOGRAPHIC_AREA));
		for (String dimItemTitle : dimItemTitles) {
			output.outputField(dimItemTitle);
		}
		output.endRow();
		this.geogObservations = new GeogObservations<String>(details.getSegmentDetails(), dimItemTitles.size(), null, String.class);
	}

	private void printTimeDimensionTitle(WdaDatasetDefintion wdaDatasetDefintion) {
		if (timeDimensionTitle == null) { // If title not found in dim item sets
			// Find time dimension
			for (WdaDimension dimension : wdaDatasetDefintion.getWdaDimensions()) {
				if ("Time".equals(dimension.getDimensionType())) {
					timeDimensionTitle = language.choose(dimension.getDimensionTitle(), dimension.getWelshDimensionTitle());
					break;
				}
			}
		}
		output.outputField(timeDimensionTitle);
	}

	@Override
	public void end(EndDetails details, Throwable callbackException) throws Throwable {
		super.end(details, callbackException);
		output.outputField(COPYRIGHT);
		output.endRow();
		output.close();
	}

	
	@Override
	public void geogStart(GeogDetails details) throws Throwable {
		output.outputField(details.getGeogCode());
		output.outputField(language.choose(details.getGeogLabel(), details.getGeogLabelWelsh()));
	}

	@Override
	public void geogEnd() throws Throwable {
		for (String observation : geogObservations.getObservations()) {
			output.outputField(observation == null ? "" : observation);
		}
		output.endRow();
		geogObservations.clear();
	}

	@Override
	public void formatObservation(Observation observation) throws Throwable {
		BigDecimal value = observation.getValue();
		String stringValue = null;
		boolean spaceFlag = false;
		if (value != null && StringUtils.isNotEmpty(observation.getStatus())) {
			WdaTranslatedDataMarking translatedDataMarking = startDetails.getTranslatedDataMarkings().get(observation.getStatus().trim());
			String translatedDataMarkingValue = translatedDataMarking != null ? translatedDataMarking.getValue() : "";
			stringValue = value.toString() + (StringUtils.isNotEmpty(translatedDataMarkingValue) ? " (" + translatedDataMarkingValue + ")" : "");
		} else if (value == null && StringUtils.isNotEmpty(observation.getStatus())) {
			WdaTranslatedDataMarking translatedDataMarking = startDetails.getTranslatedDataMarkings().get(observation.getStatus().trim());
			stringValue = translatedDataMarking != null ? translatedDataMarking.getValue() : "";
		} else if (value != null) {
			stringValue = value.toString();
			spaceFlag = true;
		} else {
			// If value and status is null, set stringValue as ""
			stringValue = "";
		}
		if (null != obsType) {
			StringBuilder builder = new StringBuilder();
			builder.append(stringValue);
			if (spaceFlag) {
				builder.append(" ");
			}
			builder.append("[");
			builder.append(obsType.getObsType());
			builder.append(null != observation.getObsTypeValue() ? observation.getObsTypeValue() : "");
			builder.append("]");
			stringValue = builder.toString();
		}
		Integer offset = offsetForDimItemSetId.get(observation.getDimItemSetId());
		if (offset != null) {
			geogObservations.addObservation(observation.getDimItemSetId(), stringValue, offset);
		} else {
			logger.warn("Offset not found for dimItemSetId " + observation.getDimItemSetId());
		}
	}

	private void determineTitles(List<String> measureTypes, List<String> statisticalUnits, List<String> dimTitles, List<String> dimItemTitles) {
		Map<String, Integer> uniqueDimItemTitles = new HashMap<String, Integer>();
		int nextOffset = 0;
		for (SegmentDetails segment : startDetails.getSegmentDetails()) {
			for (DimItemSet dimItemSet : segment.getDimItemSets()) {
				long dimItemSetId = dimItemSet.getDimItemSet().getDimItemSetId();
				StringBuilder dimTitle = new StringBuilder();
				StringBuilder dimItemTitle = new StringBuilder();
				for (WdaDimensionItem wdaDimensionItem : dimItemSet.getDimensionItemList()) {
					WdaDimension wdaDimension = wdaDimensionItem.getWdaDimension();
					if (isGeogSig || !wdaDimension.getDimensionType().equals("Time")) {
						WdaClassificationItem wdaClassificationItem = wdaDimensionItem.getWdaClassificationItem();
						dimTitle.append((dimTitle.length() == 0 ? "" : "~") + language.choose(wdaDimension.getDimensionTitle(), wdaDimension.getWelshDimensionTitle()));
						dimItemTitle.append((dimItemTitle.length() == 0 ? "" : "~") + (wdaClassificationItem.getIstotal() != null ? (language.choose("Total") + ": ") : "")
								+ (language.choose(wdaDimensionItem.getLabel(), wdaDimensionItem.getWelshLabel())));
					} else {
						if (timeDimensionTitle == null) {
							timeDimensionTitle = language.choose(wdaDimension.getDimensionTitle(), wdaDimension.getWelshDimensionTitle());
						}
					}
				}
				String key = dimItemTitle + "_" + segment.getSegmentId();
				if (uniqueDimItemTitles.containsKey(key)) {
					offsetForDimItemSetId.put(dimItemSetId, uniqueDimItemTitles.get(key));
				} else {
					measureTypes.add(segment.getMeasureType().getLabel(language));
					statisticalUnits.add(segment.getStatisticalUnit().getLabel(language));
					dimTitles.add(dimTitle.toString());
					dimItemTitles.add(dimItemTitle.toString());
					offsetForDimItemSetId.put(dimItemSetId, nextOffset);
					uniqueDimItemTitles.put(key, nextOffset++);
				}
			}
		}
	}
*/
	public void flush() throws IOException {
		output.flush();
	}

	protected class CsvWriter extends PrintWriter {
		private boolean firstColumn = true;

		public CsvWriter(PrintWriter output) {
			super(output);
		}

		public void outputField(String contents) {
			if (contents.length() > 0) {
				contents = contents.replace(DOUBLE_QUOTE, DOUBLE_QUOTE + DOUBLE_QUOTE);
				contents = DOUBLE_QUOTE + contents + DOUBLE_QUOTE;
			}
			if (!firstColumn) {
				print(COMMA);
			}
			firstColumn = false;
			print(contents);
		}

		public void endRow() {
			firstColumn = true;
			println();
		}
	}
}
