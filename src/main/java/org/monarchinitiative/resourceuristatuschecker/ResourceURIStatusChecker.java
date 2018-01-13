package org.monarchinitiative.resourceuristatuschecker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.log4j.Logger;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.common.collect.Sets;

public class ResourceURIStatusChecker {
	private static final Logger logger = Logger.getLogger(ResourceURIStatusChecker.class.getName());

	private String[] schemes = {"http","https"};
	private UrlValidator urlValidator = new UrlValidator(schemes);
	private String checkwordArr[] = {"warn","error","fatal","invalid"};
	private Set<IRI> nodeTrailSet = Sets.newHashSet();
	private Random rand = new Random();
	private Boolean flagURIVisit = false;
	
	public void run(String inputFilePath, Boolean flagURIVisit) {
		this.flagURIVisit = flagURIVisit;
		File inputFile = new File(inputFilePath);
		if (inputFile.isDirectory())
			runOverDir(inputFile);
		else
			runOverFile(inputFile);
	}

	public void runOverDir(File inputDir) {
		String[] extensions = new String[] { "ttl", "owl" };
		List<File> files = (List<File>) FileUtils.listFiles(inputDir, extensions, true);
		for (File file : files)
			runOverFile(file);
	}

	public void runOverFile(File inputFile) {
		BufferedWriter bw = null;
		FileWriter fw = null;

		String inputFilename = inputFile.getName();
		int pointPos = inputFilename.lastIndexOf(".");
		File file = new File(inputFilename.substring(0, pointPos) + "_output.log");

		try {
			if (file.exists() != true) file.createNewFile();
			fw = new FileWriter(file.getAbsoluteFile(), true);
			bw = new BufferedWriter(fw);

			FileInputStream is = new FileInputStream(inputFile);
			RDFFormat format = Rio.getParserFormatForFileName(inputFile.getName()).orElse(RDFFormat.RDFXML);
			Model model = Rio.parse(is, "", format);

			logger.info("Validating URIs of Nodes in " + inputFilename + "...") ;
			for (Statement statement: model) {
				Resource subj = statement.getSubject();
				IRI pred = statement.getPredicate();
				Value obj = statement.getObject();

				if (subj instanceof IRI)  validateNodeURI((IRI)subj, bw);
				validateNodeURI(pred, bw);
				if (obj instanceof IRI)  validateNodeURI((IRI)obj, bw);
			}

			if (flagURIVisit) {
				logger.info("Checking the status of webpages that match URIs in " + inputFilename + "...") ;
				for (IRI iri: nodeTrailSet)
					checkNodeURIStatus(iri, bw);				
			}
		} catch (Exception e1) {
			logger.error(e1.getMessage(), e1);
		} finally {
			try {
				if (bw != null) {
					bw.close();
				}

				if (fw != null) {
					fw.close();
				}
			} catch (Exception e2) {
				logger.error(e2.getMessage(), e2);
			}
		}
	}

	public void validateNodeURI(IRI node, BufferedWriter bw) {
		try {
			if (nodeTrailSet.contains(node)) return;
			String nodeURI = node.toString();
			/* logger.info("Validating " + nodeURI); */

			if (urlValidator.isValid(nodeURI)) {
				nodeTrailSet.add(node);
			} else {
				logger.info(node + " is a malformed URI");
				bw.write(node.toString() + System.lineSeparator());
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void checkNodeURIStatus(IRI node, BufferedWriter bw) {
		try {
			String iri = node.toString();
			/* logger.info("Visiting " + iri); */

			Document doc = Jsoup.connect(iri)
					.timeout(9000)
					.followRedirects(true)
					.ignoreContentType(true)
					.userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
					.referrer("http://www.google.com").get();
			String docString = doc.toString().toLowerCase();

			for (String checkWord: checkwordArr) {
				if (docString.contains(checkWord)) {
					bw.write(node.toString() + System.lineSeparator());
				}
			}

			int randSec = rand.nextInt(10) + 1;
			TimeUnit.MILLISECONDS.sleep(randSec * 100);
		} catch (Exception e1) {
			logger.error(e1.getMessage(), e1);

			try {
				bw.write(node.toString() + System.lineSeparator());
			} catch (Exception e2) {
				logger.error(e2.getMessage(), e2);
			}
		}
	}
}
