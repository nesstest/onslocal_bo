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

import javax.persistence.EntityManager;

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
	
	public void run(){
		try {
	//	File fil =	Play.application().
		File file = new File(outFile + ".csv");
		FileOutputStream out = new FileOutputStream(file);
		out.write(UTF8_BOM);
		this.output = new CsvWriter(new PrintWriter(new OutputStreamWriter(out,"UTF-8"))); 
		output.outputField(COPYRIGHT);
		output.endRow();
		output.close();
		} catch (IOException e) {
		logger.error("Failed to read the input CSV: ", e);
		throw new GLLoadException("Failed to read/write the input/output CSV: ", e);
	} catch (CSVValidationException ve) {
		throw ve;
	} catch (Exception e) {
		logger.error("Failed to load CSV file: ", e);
		throw new GLLoadException("Failed to load CSV file due to " + e.getMessage(), e);
		}
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
