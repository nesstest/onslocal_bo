package services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import play.*;

import exceptions.CSVValidationException;
import exceptions.GLLoadException;
import models.*;

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
	protected Map<Long, Integer> offsetForDimItemSetId = new HashMap<Long, Integer>();
	protected boolean isGeogSig = true;
	protected String timeDimensionTitle = null;
	EntityManager em;
	DimensionalDataSet dds;
	String outFile;
	// sort by numeric area id or extcode (levels?)
	private static String VARIABLE_VALUE_QUERY = "SELECT ga.geographic_area_id, ga.ext_code, ga.name AS area_name, ga.geographic_level_type, ti.time_period_id, ti.name AS time_name, v.variable_id, v.value_domain," +
			 " v.unit_type, v.name AS variable_name, d.value" +
			 " FROM onslocal_data_bo.geographic_area ga, onslocal_data_bo.time_period ti, onslocal_data_bo.dimensional_data_point d, onslocal_data_bo.variable v" +
			 " WHERE ga.geographic_area_id = d.geographic_area_id and d.variable_id = v.variable_id and d.time_period_id = ti.time_period_id" +
			 " AND d.dimensional_data_set_id = ?1 ORDER BY ga.geographic_area_id, ti.start_date, variable_name";
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
			String timeStamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
			dds.setModified(timeStamp);
			dds.setValidationException("");
			dds.setLoadException("");
			dds.setValidationMessage("Success");
			dds.setStatus("5-Generate-OK");
			logger.info(String.format("CSV Generation completed successfully for DDS Id: %s.", dds.getDimensionalDataSetId()));
		} catch (CSVValidationException validationException) {
			dds.setStatus("5-Generate-Failed");
			dds.setValidationMessage(validationException.getMessage());
			dds.setValidationException(validationException.getLocalizedMessage());
			logger.info(String.format("CSV Generation failed for DDS Id: %s : %s", dds.getDimensionalDataSetId(), validationException ));
		} catch (GLLoadException loadException) {
			dds.setStatus("5-Generate-Failed");
			dds.setValidationException(loadException.getMessage());
			dds.setLoadException(loadException.getMessage());
			logger.info(String.format("CSV Generation failed for DDS Id: %s : %s", dds.getDimensionalDataSetId(), loadException ));
		} finally {
			em.persist(dds);
		}
	}
	
	
	public void buildCSV(){
		try {
	//	File file = new File("/logs/" + outFile + ".csv");
		File file = Play.application().getFile("/logs/" + outFile + ".csv");
		FileOutputStream out = new FileOutputStream(file);
		out.write(UTF8_BOM);
		this.output = new CsvWriter(new PrintWriter(new OutputStreamWriter(out,"UTF-8"))); 
		List<DataDTO> results = getDataRecords(dds.getDimensionalDataSetId());
		logger.info("records found = " + results.size());
		writeHeader(results);
		writeData(results);
	//	output.endRow();
	//	output.outputField(COPYRIGHT);
	//	output.endRow();
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
			if (sametime && samearea){
				output.outputField(heading);
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
				output.outputNum(sValue);
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
		
		public void outputNum(String contents) {
		//	if (contents.length() > 0) {
		//		contents = contents.replace(DOUBLE_QUOTE, DOUBLE_QUOTE + DOUBLE_QUOTE);
		//		contents = DOUBLE_QUOTE + contents + DOUBLE_QUOTE;
		//	}
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
