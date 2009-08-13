package net.sourceforge.jwbf.mediawiki.live;

import static org.junit.Assert.assertTrue;

import java.net.URL;

import net.sourceforge.jwbf.core.bots.HttpBot;
import net.sourceforge.jwbf.mediawiki.LiveTestFather;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class HttpBotTest extends LiveTestFather {


	private Logger log = Logger.getLogger(getClass());
	private HttpBot bot;
	
	/**
	 * Setup log4j.
	 * @throws Exception a
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		PropertyConfigurator.configureAndWatch("test4log4j.properties",
				60 * 1000);
		
	}
	@Before
	public void prepare() {
		bot = new HttpBot("http://localhost/") {
		};
	}
	/**
	 * Test write and read 
	 * @throws Exception a
	 */
	@Test
	public final void findContent() throws Exception {
		URL u = new URL(getValue("wikiMW1_13_url"));
		String s = bot.getPage(u.getProtocol() + "://" + u.getHost());
		assertTrue("content shuld be longer then one", s.length() > 1);
	}
	

}