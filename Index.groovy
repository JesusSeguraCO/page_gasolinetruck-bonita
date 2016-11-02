import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.Runtime;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.naming.Context;
import javax.naming.InitialContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;

import groovy.sql.Sql
import groovy.json.JsonBuilder

import org.json.simple.JSONObject;
import org.codehaus.groovy.tools.shell.CommandAlias;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.apache.commons.lang3.StringEscapeUtils

import org.bonitasoft.console.common.server.page.PageContext
import org.bonitasoft.console.common.server.page.PageController
import org.bonitasoft.console.common.server.page.PageResourceProvider
import org.bonitasoft.engine.exception.UnknownAPITypeException;

import com.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProfileAPI;

import org.bonitasoft.engine.profile.ProfileCriterion;
import org.bonitasoft.engine.profile.Profile;



import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;

import org.bonitasoft.ext.properties.BonitaProperties;

public class Index implements PageController {

    Logger logger= Logger.getLogger("org.bonitasoft");


    String[]  listAttributs = [
        "sql",
        "datasource",
        "expl",
        "profilename",
        "testparameters",
        "delayms",
        "simulationmode",
        "simulationresult",
        "simulationdelayms"
    ];
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {



        try {
            def String indexContent;
            pageResourceProvider.getResourceAsStream("Index.groovy").withStream { InputStream s->
                indexContent = s.getText()
            };
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter()

            String action=request.getParameter("action");
            String queryId = request.getParameter("queryId");
            if (queryId!=null && action==null)
                action="query";

            logger.info("###################################### action is["+action+"] 2.0!");
            if (action==null || action.length()==0 ) {
                // logger.info("RUN Default !");

                runTheBonitaIndexDoGet( request, response,pageResourceProvider,pageContext);
                return;
            }

            APISession session = pageContext.getApiSession()
            ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(session);
            ProfileAPI profileAPI =   TenantAPIAccessor.getProfileAPI(session);

            Map<String,Object> answer = null;
            def answerrows=null;
            List<BEvent> listEvents = new ArrayList<BEvent>();
            String jsonparam=request.getParameter("json");
            logger.info(" jsonSt["+jsonparam+"]");
            final HashMap<String, Object>  jsonHash=null;
            if (jsonparam != null && jsonparam.length() > 0 ) {
                jsonHash = (HashMap<String, Object>) JSONValue.parse( jsonparam );
            }

            if ("savequery".equals(action))	{

                answer = new HashMap<String,Object>();
                if (jsonHash!=null) {
                    try {
                        BonitaProperties bonitaProperties = new BonitaProperties( pageResourceProvider );

                        listEvents.addAll( bonitaProperties.load() );


                        // replace the old id by the new one
                        Set<String> setqueriesid = decodeListQuery( bonitaProperties.getProperty( "listqueries" ) );

                        String oldId= jsonHash.get("oldId");
                        String id= jsonHash.get("id");

                        logger.info("Id["+id+"] oldId["+oldId+"] Id exist ? "+setqueriesid.contains(id)+" setqueriesid["+setqueriesid+"]");

                        if (setqueriesid.contains(id) && (oldId==null || ! oldId.equals( id ) ))
                        {
                            // the new ID already exist
                            listEvents.add( new BEvent("com.bonitasoft.gasoline", 4, Level.APPLICATIONERROR, "ID already exist", "This ID already exist, and you can't have 2 requests with the same ID", "The query is not saved", "Change and choose an new id"));
                        }
                        else
                        {
                            if (oldId!=null && ! oldId.equals( id ) )
                            {
                                // remove the oldId in the list
                                logger.info("Id change, remove the oldid["+oldId+"] in ["+setqueriesid+"]");
                                setqueriesid.remove(oldId );

                                logger.info("new listQueriesId["+setqueriesid+"]");
                                for (String attr : listAttributs)
                                {
                                    bonitaProperties.remove( oldId+"_"+attr);
                                }
                            }
                            setqueriesid.add( id );

                            logger.info("new listqueriesid["+setqueriesid+"]");

                            for (String attr : listAttributs)
                            {
                                logger.info("Save attr["+attr+"] value=["+ jsonHash.get(attr)+"]");
                                
                                bonitaProperties.setProperty( id+"_"+attr, (jsonHash.get(attr)==null ? "" :  jsonHash.get(attr)));
                                /*
                                 bonitaProperties.setProperty( id+"_datasource", (jsonHash.get("datasource")==null ? "" :  jsonHash.get("datasource")) );
                                 bonitaProperties.setProperty( id+"_expl" , ( jsonHash.get("expl") == null ? "" :  jsonHash.get("expl")));
                                 bonitaProperties.setProperty( id+"_profilename" , ( jsonHash.get("profilename") == null ? "" :  jsonHash.get("profilename")));
                                 bonitaProperties.setProperty( id+"_testparameters" , ( jsonHash.get("testparameters") == null ? "" :  jsonHash.get("testparameters")));
                                 bonitaProperties.setProperty( id+"_delayms" , ( jsonHash.get("delayms") == null ? "" :  jsonHash.get("delayms")));
                                 bonitaProperties.setProperty( id+"_simulationmode",( jsonHash.get("simulationmode") == null ? "" :  jsonHash.get("simulationmode"))); );
                                 bonitaProperties.setProperty( id+"_simulationresult",( jsonHash.get("simulationresult") == null ? "" :  jsonHash.get("simulationresult"))); );
                                 bonitaProperties.setProperty( id+"_simulationdelayms",( jsonHash.get("simulationdelayms") == null ? "" :  jsonHash.get("simulationdelayms"))); );
                                 */
                            }
                            bonitaProperties.setProperty( "listqueries", codeListQueries( setqueriesid ));

                            listEvents.addAll(  bonitaProperties.store());

                            if (! BEventFactory.isError( listEvents ))
                            {
                                listEvents.add( new BEvent("com.bonitasoft.gasoline", 1, Level.SUCCESS, "Queries saved", "the properties is saved with success"));
                                answer.put("id",id );

                            }

                        }
                    }
                    catch( Exception e ) {
                        StringWriter sw = new StringWriter();
                        e.printStackTrace(new PrintWriter(sw));
                        String exceptionDetails = sw.toString();
                        logger.severe("SaveQuery:Exception "+e.toString()+" at "+exceptionDetails);

                        listEvents.add( new BEvent("com.bonitasoft.ping", 2, Level.APPLICATIONERROR, "Error using BonitaProperties", "Error :"+e.toString(), "Properties is not saved", "Check exception"));
                    }

                    List<Map<String,String>> listQueries = new ArrayList<Map<String,String>>();
                    loadQueries(  pageResourceProvider, listQueries,  listEvents);
                    answer.put("listqueries",listQueries );
                }
                else
                    listEvents.add( new BEvent("com.bonitasoft.ping", 3, Level.APPLICATIONERROR, "JsonHash can't be decode", "the parameters in Json can't be decode", "Properties is not saved", "Check page"));
            }
            else  if ("loadqueries".equals(action)) {
                answer = new HashMap<String,Object>();
                try {
                    logger.info("loadqueries");
                    List<Map<String,String>> listQueries = new ArrayList<Map<String,String>>();
                    loadQueries(  pageResourceProvider, listQueries,  listEvents);
                    answer.put("listqueries",listQueries );
                }
                catch( Exception e ) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    String exceptionDetails = sw.toString();
                    logger.severe("LoadQuery:Exception "+e.toString()+" at "+exceptionDetails);

                    listEvents.add( new BEvent("com.bonitasoft.gasolinetruck", 10, Level.APPLICATIONERROR, "Error loading queries", "Error :"+e.toString(), "Properties is not saved", "Check exception"));
                }
            }
            else if ("removequery".equals(action))
            {
                answer = new HashMap<String,Object>();
                try
                {
                    BonitaProperties bonitaProperties = new BonitaProperties( pageResourceProvider );
                    listEvents.addAll( bonitaProperties.load() );


                    // replace the old id by the new one
                    Set<String> setqueriesid = decodeListQuery( bonitaProperties.getProperty( "listqueries" ) );

                    String id= jsonHash.get("id");
                    if (id!=null  )
                    {
                        // remove the oldId in the list
                        logger.info("remove the id["+id+"] in ["+setqueriesid+"]");
                        setqueriesid.remove(id );

                        logger.info("new listQueriesId["+setqueriesid+"]");
                        for (String attr : listAttributs)
                        {
                            bonitaProperties.remove( id+"_"+attr);
                        }
                        /*
                         bonitaProperties.remove( id+"_sql");
                         bonitaProperties.remove( id+"_datasource");
                         bonitaProperties.remove( id+"_expl" );
                         bonitaProperties.remove( id+"_profilename" );
                         bonitaProperties.remove( id+"_testparameters" );
                         bonitaProperties.remove( id+"_delayms" );
                         */
                        bonitaProperties.setProperty( "listqueries", codeListQueries( setqueriesid ));


                        listEvents.addAll(  bonitaProperties.store());
                    }


                }
                catch( Exception e ) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    String exceptionDetails = sw.toString();
                    logger.severe("LoadQuery:Exception "+e.toString()+" at "+exceptionDetails);

                    listEvents.add( new BEvent("com.bonitasoft.gasolinetruck", 10, Level.APPLICATIONERROR, "Error loading queries", "Error :"+e.toString(), "Properties is not saved", "Check exception"));
                }
                List<Map<String,String>> listQueries = new ArrayList<Map<String,String>>();
                loadQueries(  pageResourceProvider, listQueries,  listEvents);
                answer.put("listqueries",listQueries );

            }
            else if ("query".equals(action) || "testquery".equals(action)) {
                // execute a query ?
                answer = new HashMap<String,Object>();

                if (queryId == null)
                    queryId= jsonHash.get("id");
                
                // Get the query SQL definition from the queries.properties file using query id.
                BonitaProperties bonitaProperties = new BonitaProperties( pageResourceProvider );
                listEvents.addAll( bonitaProperties.load() );
                String querySql                = bonitaProperties.getProperty( queryId + "_sql" );
                String datasource            = bonitaProperties.getProperty( queryId + "_datasource" );
                String simulationMode   = bonitaProperties.getProperty( queryId + "_simulationmode" );
                String simulationResult   = bonitaProperties.getProperty( queryId + "_simulationresult" );
                

                if ( "testquery".equals(action))
                {
                    querySql = (jsonHash.get("sql")==null ? "" :  jsonHash.get("sql"));
                    datasource = (jsonHash.get("datasource")==null ? "" :  jsonHash.get("datasource"));
                }



                Long delayms=null;
                try {
                    delayms= Long.valueOf( bonitaProperties.getProperty( queryId+"_delayms" ));
                } catch(Exception e )
                {
                    logger.info("getdelayms : "+e.toString());
                };

                boolean continueoperation=true;
                // If a profile is required ?
                String profilename = bonitaProperties.getProperty( queryId+"_profilename" );
                if (profilename != null && profilename.length()>0)
                {
                    logger.info("Check if the user has the profile ["+profilename+"]");
                    boolean permission=false;
                    List<Profile>  listProfiles  = profileAPI.getProfilesForUser( session.getUserId(),0,10000, ProfileCriterion.ID_ASC   );
                    for (Profile profile : listProfiles)
                    {
                        if (profile.getName() .equals( profilename ))
                            permission=true;
                    }
                    if (!permission)
                    {
                        logger.info("The user ["+session.getUserId()+"] does not have the profilename["+profilename+"]");
                        listEvents.add( new BEvent("com.bonitasoft.gasolinetruck", 22, Level.APPLICATIONERROR, "User is not registerd in the profile", "This query is protected by a profile name, and the user is not registered inside", "Query is rejected", "Register the user in the profile (profile name is not given for security reason)"));
                        if ( ! "testquery".equals(action))
                        {
                            response.setStatus( 403 );
                            return;
                        }
                        continueoperation=false;
                    }
                    else
                        logger.info("He has the profile ["+profilename+"]");
                }



                if (queryId==null && querySql == null) {
                    answer.put("status", "No queryid.");
                    listEvents.add( new BEvent("com.bonitasoft.gasolinetruck", 20, Level.APPLICATIONERROR, "No queryId given as parameters","The queryid is mandatory","query can't run","Set a queryId"));
                    continueoperation=false;
                }
                else if (querySql == null) {
                    answer.put("status", "The queryId does not refer to an existing query. Check your query id and queries.properties file content.");
                    listEvents.add( new BEvent("com.bonitasoft.gasolinetruck", 21, Level.APPLICATIONERROR, "queryId ["+queryId+"] does not refer to an existing query", "No query runs", "look the list of existing query"));
                    continueoperation=false;
                }
                if ( continueoperation )
                {
                    // Build a map will all SQL queries parameters (all REST call parameters expect "queryId").
                    Map<String, String> params = getSqlParametersFromUrl( request )
                    logger.info("Run queryId["+queryId+"]  SqlQuery["+querySql+"] in  datasource["+datasource +"] params["+params+"] simulationMode ["+simulationMode+"]");
                    String message="";

                    boolean doSimulation="always".equals(simulationMode)
                    List rows=null;
                    long beginTime = System.currentTimeMillis();
                    String sqlRequest=null;
                    if ( ! doSimulation)
                    {
                        // run the sql
                        // Get the database connection using the data source declared in datasource.properties
                        Sql sql = buildSql(  datasource );
                        sqlRequest = params.isEmpty() ? querySql : querySql+" [Param "+params+"]";
                        try {
                            logger.info("play rows" );
                            // Run the query with or without parameters.
                            rows = params.isEmpty() ? sql.rows(querySql) : sql.rows(querySql, params)
                            if (delayms != null)
                            {
                                logger.info("Delay asked, sleep "+delayms+" ms")
                                Thread.sleep( delayms );
                            };
                   

                             

                            // JsonBuilder builder = new JsonBuilder(rows)
                            // String table = builder.toPrettyString()
                            // return buildResponse(responseBuilder, table)
                        } catch (Exception e)
                        {
                            StringWriter sw = new StringWriter();
                            e.printStackTrace(new PrintWriter(sw));
                            String exceptionDetails = sw.toString();
                            logger.severe("LoadQuery:Exception "+e.toString()+" at "+exceptionDetails);
                            doSimulation="onerror".equals(simulationMode);

                            if (!doSimulation)
                            {
                                answer = new HashMap<String,Object>();
                                listEvents.add( new BEvent("com.bonitasoft.gasolinetruck", 22, Level.APPLICATIONERROR, "SQLError "+e.getMessage(), "SqlQuery["+sqlRequest+"] can't give a result", "Check the query"));
                            }

                        } finally {
                            sql.close()
                        }
                    } // end run sql
                    if (doSimulation)
                    {
                        logger.info("LoadQuery: do simulation !");
                        Long delaySimulationms=null;
                        try {
                            delaySimulationms= Long.valueOf( bonitaProperties.getProperty( queryId+"_simulationdelayms" ));
                            Thread.sleep( delaySimulationms );
                        } catch(Exception e )
                        {
                            logger.info("simulationdelayms : "+e.toString());
                        };
                        String  result=  bonitaProperties.getProperty( queryId+"_simulationresult" );
                        logger.info("result["+result+"]");
                        try
                        {
                            if (result != null && result.length() > 0 ) {
                                logger.info("Parse ");                                
                                rows =  (List<String, Object>) JSONValue.parse( result );
                                logger.info("Parse result : row=["+rows+"]");
                                if (rows==null)
                                    message="Parsing error on simulation Result;";
                            }
                        }
                        catch(Exception e) {
                            logger.severe("Error on result ["+e+"]");
                            message+="Error on Parsing :"+e.toString()+";";
                            
                            listEvents.add( new BEvent("com.bonitasoft.gasolinetruck", 23, Level.APPLICATIONERROR, "Simulation error "+e.getMessage(), "Result sould be a JSON Array", "Check the simulation"));

                        }

                    }
                    long endTime = System.currentTimeMillis();
                    logger.info(" doSimulation ? "+doSimulation+"] sql["+sqlRequest+"] executed in ["+ (endTime-beginTime)+"] ms action[" + action+"] message["+message+"]" );
                    
                    logger.info("return >>> rows["+rows+"] message");
                    if (message.length()>0)
                        listEvents.add( new BEvent("com.bonitasoft.gasolinetruck", 24, Level.APPLICATIONERROR, "Error "+message, "An error occure", "Check the message"));
                    
                    // Build the JSON answer with the query result
                    if ( "testquery".equals(action))
                    {
                        answer.put("rows",rows);
                        answer.put("stats", (endTime-beginTime));


                    }
                    else
                    {
                        answerrows = rows;
                    }
                    
                    
                } // end continue operation

            } // end execute a query

            // return the result now
            if (answerrows != null)
            {
                
                JsonBuilder builder = new JsonBuilder(answerrows);
                String table = builder.toPrettyString();
                logger.info("return answerrows:["+table+"]");
                
                out.write( table );
                out.flush();
                out.close();
                return
            }
            if (answer!=null)
            {
                answer.put("listevents", BEventFactory.getHtml(listEvents) );

                String jsonDetailsSt = JSONValue.toJSONString( answer );
                logger.info("return Json:["+jsonDetailsSt+"]");
                
                out.write( jsonDetailsSt );
                out.flush();
                out.close();
                return;
            }
            out.write( "Unknow command" );
            out.flush();
            out.close();
            return;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            logger.severe("Exception ["+e.toString()+"] at "+exceptionDetails);
        }
    }


    /** -------------------------------------------------------------------------
     *
     *runTheBonitaIndexDoGet
     * 
     */
    private void runTheBonitaIndexDoGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
        try {
            def String indexContent;
            pageResourceProvider.getResourceAsStream("index.html").withStream { InputStream s->
                indexContent = s.getText()
            }

            // def String pageResource="pageResource?&page="+ request.getParameter("page")+"&location=";
            // indexContent= indexContent.replace("@_USER_LOCALE_@", request.getParameter("locale"));
            // indexContent= indexContent.replace("@_PAGE_RESOURCE_@", pageResource);

            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            out.print(indexContent);
            out.flush();
            out.close();
        } catch (Exception e) {
            logger.severe("Exception "+e);
        }
    }

    /**
     * load all queries
     * populate the listQueries and the listEvents
     */
    private void loadQueries( PageResourceProvider pageResourceProvider, List<Map<String,String>> listQueries,  List<BEvent> listEvents)
    {
        BonitaProperties bonitaProperties = new BonitaProperties( pageResourceProvider );
        listEvents.addAll( bonitaProperties.load() );
        logger.info("Load done, events = "+listEvents.size() );


        Set<String> setqueriesid = decodeListQuery( bonitaProperties.getProperty( "listqueries" ) );

        for (String id : setqueriesid) {
            Map<String,Object> oneQuery = new HashMap<String,Object>();
            oneQuery.put("id", id );
            oneQuery.put("oldId", id );
            for (String attr : listAttributs)
            {
                oneQuery.put( attr, , bonitaProperties.getProperty( id+"_"+attr ));
            }
            /*
            oneQuery.put("sql", bonitaProperties.getProperty( id+"_sql" ));
            oneQuery.put("datasource", bonitaProperties.getProperty( id+"_datasource" ));
            oneQuery.put("expl", bonitaProperties.getProperty( id+"_expl" ));
            oneQuery.put("profilename", bonitaProperties.getProperty( id+"_profilename" ));
            oneQuery.put("testparameters", bonitaProperties.getProperty( id+"_testparameters" ));
            oneQuery.put("delayms", bonitaProperties.getProperty( id+"_delayms" ));
            oneQuery.put("profile", bonitaProperties.getProperty( id+"_profile" ));
            */
            listQueries.add (oneQuery );
        }
        return;

    }

    protected Map<String, String> getSqlParametersFromUrl(HttpServletRequest request) {
        Map<String, String> params = [:];

        for (String parameterName : request.getParameterNames()) {
            if (parameterName !=null && parameterName.length()>0)
            {
                params.put(parameterName, request.getParameter(parameterName));
            }
        }
        params.remove("queryId");
        params.remove("action");
        params.remove("page");
        params.remove("json");
        return params
    }

    protected Sql buildSql(  datasource ) {
        Context ctx = new InitialContext()
        DataSource dataSource = (DataSource) ctx.lookup( datasource )
        return new Sql(dataSource)
    }

    private Set<String> decodeListQuery(   String listqueriesString )
    {
        Set<String> listQueries = new HashSet<String>();
        if (listqueriesString == null)
            return listQueries;
        StringTokenizer st = new StringTokenizer(listqueriesString,"#");

        while (st.hasMoreTokens()) {
            String id = st.nextToken();
            listQueries.add( id );
        }
        return listQueries;
    }
    private String codeListQueries( Set<String > listQueries )
    {
        String list="";
        boolean sep=false;
        for (String key: listQueries)
        {
            list = list+ (sep ? "#":"")+key;
            sep=true;
        }
        return list;
    }



}
