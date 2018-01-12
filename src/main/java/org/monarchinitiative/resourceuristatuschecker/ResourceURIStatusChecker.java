package org.monarchinitiative.resourceuristatuschecker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.FileManager;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ResourceURIStatusChecker {
	private static final Logger logger = Logger.getLogger(ResourceURIStatusChecker.class.getName());
	private String checkwordArr[] = {"warn","error","fatal","invalid"};
	private Set<RDFNode> nodeTrailSet = Sets.newHashSet();
	private Random rand = new Random();
	private String[] schemes = {"http","https"};
	private UrlValidator urlValidator = new UrlValidator(schemes);

	public void run(String inputFilePath) {
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

			Model onetimeModel = FileManager.get().loadModel(inputFile.toString());
			String queryString = "SELECT * WHERE { ?s ?p ?o }";
			Query query = QueryFactory.create(queryString);
			QueryExecution qexec = QueryExecutionFactory.create(query, onetimeModel);
			ResultSet results = qexec.execSelect() ;
			long modelSize = onetimeModel.size();

			logger.info("Model size: " + modelSize);
			logger.info("Validating URIs of Nodes ....");
			for ( ; results.hasNext() ; ) {
				QuerySolution soln = results.nextSolution();
				validateNodeURI(soln.get("s"), bw);
				validateNodeURI(soln.get("p"), bw);
				validateNodeURI(soln.get("o"), bw);
			}
			
			long nodeSetSize = nodeTrailSet.size();
			logger.info("Checking the status of URIs of Nodes ....");
			logger.info("#Nodes: " + nodeSetSize);
			for (RDFNode node : nodeTrailSet) {
				checkNodeURIStatus(node, bw);
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

	public void validateNodeURI(RDFNode node, BufferedWriter bw) {
		try {
			if (nodeTrailSet.contains(node)) return;
			if (node.isLiteral()) return;
			if (node.isAnon()) return;
			
			String nodeURI = node.asNode().getURI();
			logger.info("Validating " + nodeURI);
			
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

	public void checkNodeURIStatus(RDFNode node, BufferedWriter bw) {
		try {
			String URI = node.asResource().getURI();
			logger.info("Visiting " + URI);

			Document doc = Jsoup.connect(URI)
					.timeout(6000)
					.followRedirects(true)
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
