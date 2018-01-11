package org.monarchinitiative.resourceuristatuschecker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
	private static final String outputFilename = "output.log";
	private String checkwordArr[] = {"warning","error","fatal","invalid"};
	private Set<RDFNode> nodeTrailSet = Sets.newHashSet();
	private Random rand = new Random();

	public void run(String inputFilePath) {
		BufferedWriter bw = null;
		FileWriter fw = null;
		File file = new File(outputFilename);

		try {
			if (file.exists() != true) file.createNewFile();
			fw = new FileWriter(file.getAbsoluteFile(), true);
			bw = new BufferedWriter(fw);

			Model onetimeModel = FileManager.get().loadModel(inputFilePath);
			String queryString = "SELECT * WHERE { ?s ?p ?o }";
			Query query = QueryFactory.create(queryString);
			QueryExecution qexec = QueryExecutionFactory.create(query, onetimeModel);
			ResultSet results = qexec.execSelect() ;

			for ( ; results.hasNext() ; ) {
				QuerySolution soln = results.nextSolution();
				checkNodeURI(soln.get("s"), bw);
				checkNodeURI(soln.get("p"), bw);
				checkNodeURI(soln.get("o"), bw);
			}

		} catch (Exception e1) {
			logger.error(e1.getMessage(), e1);
		} finally {
			try {
				if (bw != null) {
					bw.flush();
					bw.close();
				}

				if (fw != null) {
					fw.flush();
					fw.close();
				}
			} catch (Exception e2) {
				logger.error(e2.getMessage(), e2);
			}
		}
	}

	public void checkNodeURI(RDFNode node, BufferedWriter bw) {
		if (nodeTrailSet.contains(node)) return;
		if (node.isLiteral()) return;
		if (node.isAnon()) return;

		try {
			String URI = node.asResource().getURI();
			logger.info("Checking " + URI);

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
		} finally {
			nodeTrailSet.add(node);
		}
	}
}
