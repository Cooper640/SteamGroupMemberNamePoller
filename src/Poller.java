import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.apache.commons.lang3.StringEscapeUtils;

public class Poller {
	
	private static boolean morePages = false;

	public static void main(String[] args) {
		if(args.length == 0){
			args = new String[1];
			args[0] = "jamandbeags";
		}
		try {
			URL url = new URL("http://steamcommunity.com/groups/"+args[0]+"/memberslistxml/?xml=1");
			URLConnection conn = url.openConnection();

			DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dfactory.newDocumentBuilder();
			InputStream is = conn.getInputStream();
			Document doc = builder.parse(is);
			is.close();
			
			List<String> steamIDs = new ArrayList<String>();
			
			steamIDs.addAll(pullIDs(doc));
			
			checkForMorePages(doc);
			
			while(morePages){
				Node urlNode = doc.getElementsByTagName("nextPageLink").item(0);
				String urlString = urlNode.getTextContent();
				url = new URL(urlString);
				conn = url.openConnection();

				dfactory = DocumentBuilderFactory.newInstance();
				builder = dfactory.newDocumentBuilder();
				is = conn.getInputStream();
				doc = builder.parse(is);
				is.close();

				steamIDs.addAll(pullIDs(doc));
				
				checkForMorePages(doc);
			}
			
			List<String> steamNames = new ArrayList<String>();
			Integer count = 0;
			for(String id : steamIDs){
				count++;
				System.out.print(count.toString());
				if(id != null && !id.isEmpty()){
					System.out.println(" http://steamcommunity.com/profiles/"+id+"/?xml=1");
					url = new URL("http://steamcommunity.com/profiles/"+id+"/?xml=1");
					conn = url.openConnection();
	
					dfactory = DocumentBuilderFactory.newInstance();
					builder = dfactory.newDocumentBuilder();
					is = conn.getInputStream();
					doc = builder.parse(is);
					is.close();
					
					NodeList nameNodes = doc.getElementsByTagName("steamID");
					if(nameNodes.getLength() == 1){
						String steamName = nameNodes.item(0).getTextContent();
						String unescapedName = StringEscapeUtils.unescapeXml(steamName);
						if(steamName != null && !steamName.isEmpty()){
							steamNames.add(unescapedName);
						}
					}
				}
			}

			System.out.println(steamNames.toString());

			Path out = Paths.get("output.txt");
			Files.write(out,steamNames,Charset.forName("UTF-8"));
			
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		}
		

	}
	
	private static List<String> pullIDs(Document doc){
		List<String> ids = new ArrayList<String>();
		
		NodeList idNodes = doc.getElementsByTagName("steamID64");
		
		for(int i = 0; i < idNodes.getLength(); i++){
			ids.add(idNodes.item(i).getTextContent());
		}
		
		return ids;
	}
	
	private static void checkForMorePages(Document doc){
		if(doc.getElementsByTagName("nextPageLink").getLength() > 0){
			morePages = true;
		}
		else{
			morePages = false;
		}
	}

}
