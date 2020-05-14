package com.utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Request;

import org.glassfish.jersey.media.multipart.FormDataParam;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Explores the Java classes in a given package, looking for annotations
 * indicating REST endpoints. These are written to an HTML table, documenting
 * basic information about all the known endpoints.
 */
public class RESTEndpointsDocumenter {

    // used to store the collection of attributes for a web services endpoint
    public static final String NEWLINE = System.getProperty("line.separator");

    public static final String UserDirectory = System.getProperty("user.dir");

    public static final String BASE_URI = "Server/api";

    enum MethodEnum {GET, POST, PUT, DELETE}

    enum ParameterType {PATH, QUERY, PAYLOAD}

    public static void main(String[] args) {
        try {
            // the root package where Java classes implementing web services
            // endpoints can be found - the place to start the search from
            String packageName = "com.server.api";

            // the file location where the HTML table listing endpoints
            //  information will be written
            // this should be in a directory that already exists, and contains
            //  the unzipped contents of
            //  http://dalelane.co.uk/files/120114-datatables-assets.zip
            final String destinationHtmlPath = UserDirectory + "/docs/jersey-api-index.html";

            final RESTEndpointsDocumenter endpointsDocumenter = new RESTEndpointsDocumenter();
            final List<RestEndpoint> restEndpoints = endpointsDocumenter.findRESTEndpoints(packageName);
            final File endpointsDoc = endpointsDocumenter.outputEndpointsTable(restEndpoints, destinationHtmlPath);

            System.out.println("Api-Index is complete. HTML file written to " + endpointsDoc.getAbsolutePath());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the provided REST endpoints to an HTML file.
     */
    public File outputEndpointsTable(final List<RestEndpoint> restEndpoints, final String htmlPath) throws IOException {
        final File docFile = new File(htmlPath);

        checkHtmlAssetFiles(docFile.getAbsoluteFile().getParentFile());

        final FileWriter fStream = new FileWriter(docFile);
        final BufferedWriter out = new BufferedWriter(fStream);

        out.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">"
                + NEWLINE);
        out.write("<html>");
        out.write("<head>");

        out.write("<style type=\"text/css\">" + NEWLINE);
        out.write("@import \"api_page.css\";" + NEWLINE);
        out.write("@import \"header.css\";" + NEWLINE);
        out.write("@import \"api_table.css\";" + NEWLINE);
        out.write("</style>" + NEWLINE);

        out.write("<script type=\"text/javascript\" charset=\"utf-8\" src=\"jquery.js\"></script>" + NEWLINE);
        out.write(
                "<script type=\"text/javascript\" charset=\"utf-8\" src=\"jquery.dataTables.js\"></script>" + NEWLINE);
        out.write("<script type=\"text/javascript\" charset=\"utf-8\" src=\"FixedColumns.js\"></script>" + NEWLINE);
        out.write("<script type=\"text/javascript\" charset=\"utf-8\" src=\"RowGroupingWithFixedColumn.js\"></script>"
                + NEWLINE);

        out.write("</head>" + NEWLINE);

        out.write("<body id=\"dt_example\">" + NEWLINE);
        out.write("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"display\" id=\"endpoints\">"
                + NEWLINE);
        out.write("<thead><tr>");
        out.write("<th>REST URI</th>");
        out.write("<th>Method</th>");
        out.write("<th>Java REST Class</th>");
        out.write("<th>Java Class Method</th>");
        out.write("<th>API REST Parameters</th>");
        out.write("<th>REST Data Format</th>");
        out.write("<th>REST Responses</th>");
        out.write("<th>API-Tags</th>");
        out.write("<th>Annotations</th>");
        out.write("<th>Descriptions</th>");
        out.write("</tr>" + NEWLINE);
        out.write("</head>" + NEWLINE);
        out.write("<tbody>" + NEWLINE);

        for (final RestEndpoint restEndpoint : restEndpoints) {
            switch (restEndpoint.method) {
                case GET:
                    out.write("<tr class='gradeA'>");
                    break;
                case POST:
                    out.write("<tr class='gradeU'>");
                    break;
                case PUT:
                    out.write("<tr class='gradeC'>");
                    break;
                case DELETE:
                    out.write("<tr class='gradeX'>");
                    break;
                default:
                    out.write("<tr>");
            }

            out.write("<td>" + restEndpoint.uri + "</td>");
            out.write("<td>" + restEndpoint.method + "</td>");
            out.write("<td>" + restEndpoint.javaClass + "</td>");
            out.write("<td>" + restEndpoint.javaMethodName + "</td>");

            // Parameters Column
            out.write("<td class='column-content-small'>");
            for (EndpointParameter parameter : restEndpoint.pathParameters) {
                out.write("Path: {" + parameter.name + "} (" + parameter.javaType + ")");
                if (parameter.defaultValue != null && parameter.defaultValue.trim().length() > 0) {
                    out.write(" Default = \"" + parameter.defaultValue + "\"");
                }
                out.write("<br/>");
            }
            for (EndpointParameter parameter : restEndpoint.queryParameters) {
                out.write("Query: {" + parameter.name + "} (" + parameter.javaType + ") ");
                if (parameter.defaultValue != null && parameter.defaultValue.trim().length() > 0) {
                    out.write(" Default = \"" + parameter.defaultValue + "\"");
                }
                out.write("<br/>");
            }
            for (EndpointParameter parameter : restEndpoint.payloadParameters) {
                if (parameter.name != null && parameter.name.trim().length() > 0) {
                    out.write("Payload: {" + parameter.name + "} (" + parameter.javaType + ") ");
                }
                else {
                    out.write("Payload: " + parameter.javaType);
                }
                out.write("<br/>");
            }
            out.write("</td>");

            // Data Format Column
            out.write("<td>");
            if (restEndpoint.consumes.size() > 0) {
                out.write("Consumes: ");
                for (String consume : restEndpoint.consumes) {
                    out.write(consume);
                    if (restEndpoint.consumes.indexOf(consume) != restEndpoint.consumes.size() - 1) {
                        out.write(", ");
                    }
                }
                out.write("<br/>");
            }
            if (restEndpoint.produces.size() > 0) {
                out.write("Produces: ");
                for (String produce : restEndpoint.produces) {
                    out.write(produce);
                    if (restEndpoint.produces.indexOf(produce) != restEndpoint.produces.size() - 1) {
                        out.write(", ");
                    }
                }
            }
            out.write("</td>");

            // Return Response Column
            out.write("<td>" + restEndpoint.javaMethodReturnType + "<br/>");
            if (restEndpoint.successResponseTypes.size() > 0) {
                out.write("Success: ");
                for (String returnType : restEndpoint.successResponseTypes) {
                    out.write(returnType);
                    if (restEndpoint.successResponseTypes.indexOf(returnType)
                            != restEndpoint.successResponseTypes.size() - 1) {
                        out.write(", ");
                    }
                }
                out.write("<br/>");
            }
            if (restEndpoint.failureResponseTypes.size() > 0) {
                out.write("Failure: ");
                for (String returnType : restEndpoint.failureResponseTypes) {
                    out.write(returnType);
                    if (restEndpoint.failureResponseTypes.indexOf(returnType)
                            != restEndpoint.failureResponseTypes.size() - 1) {
                        out.write(", ");
                    }
                }
            }
            out.write("</td>");

            // Tags Column
            out.write("<td>");
            for (String tag : restEndpoint.tags) {
                if (tag != null && tag.trim().length() > 0) {
                    out.write("@" + tag);
                }
                out.write("<br/>");
            }
            out.write("</td>");

            // Annotations Column
            out.write("<td class='column-content-small'>");
            for (String annotation : restEndpoint.annotations) {
                out.write(annotation);
                out.write("<br/>");
            }
            out.write("</td>");

            // Description Column
            out.write("<td>");
            if (restEndpoint.description != null) {
                out.write(restEndpoint.description);
            }
            if (restEndpoint.notes != null && restEndpoint.notes.trim().length() > 0) {
                out.write("<br/>");
                out.write("Note:" + restEndpoint.notes);
            }
            out.write("</td>");

            // Row End
            out.write("</tr>" + NEWLINE);
        }

        out.write("</tbody>" + NEWLINE);
        out.write("</table>" + NEWLINE);
        out.write("</body></html>");

        out.close();
        fStream.close();

        return docFile;
    }

    /**
     * Verifies that the JS and CSS files required by the HTML table are present.
     */
    private void checkHtmlAssetFiles(final File directory) throws FileNotFoundException {
        if (directory.exists() == false) {
            throw new FileNotFoundException(directory.getAbsolutePath());
        }

        final List<String> assetFiles = new ArrayList<>();
        assetFiles.add("RowGroupingWithFixedColumn.js");
        assetFiles.add("FixedColumns.js");
        assetFiles.add("api_page.css");
        assetFiles.add("api_table.css");
        assetFiles.add("header.css");
        assetFiles.add("jquery.dataTables.js");
        assetFiles.add("jquery.js");

        for (final String file : assetFiles) {
            final File asset = new File(directory, file);
            if (asset.exists() == false) {
                throw new FileNotFoundException(asset.getAbsolutePath());
            }
        }
    }

    /**
     * Returns REST endpoints defined in Java classes in the specified package.
     */
    @SuppressWarnings("rawtypes")
    public List<RestEndpoint> findRESTEndpoints(final String basePackage) throws IOException, ClassNotFoundException {
        final List<RestEndpoint> restEndpoints = new ArrayList<RestEndpoint>();
        final List<Class> classes = getClasses(basePackage);
        for (final Class<?> clazz : classes) {
            final Annotation annotation = clazz.getAnnotation(Path.class);
            if (annotation != null) {
                final String basePath = getRESTEndpointPath(clazz);
                final Method[] methods = clazz.getMethods();
                for (final Method method : methods) {
                    if (method.isAnnotationPresent(GET.class)) {
                        restEndpoints.add(createEndpoint(method, MethodEnum.GET, clazz, basePath));
                    }
                    else if (method.isAnnotationPresent(PUT.class)) {
                        restEndpoints.add(createEndpoint(method, MethodEnum.PUT, clazz, basePath));
                    }
                    else if (method.isAnnotationPresent(POST.class)) {
                        restEndpoints.add(createEndpoint(method, MethodEnum.POST, clazz, basePath));
                        // TODO: Payload validations can be listed and find out the possible failure here
                    }
                    else if (method.isAnnotationPresent(DELETE.class)) {
                        restEndpoints.add(createEndpoint(method, MethodEnum.DELETE, clazz, basePath));
                    }
                }
            }
        }
        return restEndpoints;
    }


    /**
     * Create an endpoint object to represent the REST endpoint defined in the
     * specified Java method.
     */
    private RestEndpoint createEndpoint(final Method javaMethod, final MethodEnum restMethod, final Class<?> clazz,
            final String classUri) {
        final RestEndpoint newRestEndpoint = new RestEndpoint();
        newRestEndpoint.method = restMethod;
        newRestEndpoint.javaMethodName = javaMethod.getName();
        newRestEndpoint.javaClass = clazz.getName();
        newRestEndpoint.javaMethodReturnType = javaMethod.getReturnType().getName();

        final Path path = javaMethod.getAnnotation(Path.class);
        if (path != null) {
            newRestEndpoint.uri = classUri + path.value();
        }
        else {
            newRestEndpoint.uri = classUri;
        }

        discoverParameters(javaMethod, newRestEndpoint);

        discoverSwaggerDocInfos(javaMethod, clazz, newRestEndpoint);

        newRestEndpoint.annotations = discoverAnnotations(javaMethod);

        // NOTE: Ignoring unwanted annotationsremoveAnnotations
        final List<String> removeAnnotations = new ArrayList<>();
        removeAnnotations.add(Path.class.getName());
        removeAnnotations.add(GET.class.getName());
        removeAnnotations.add(POST.class.getName());
        removeAnnotations.add(PUT.class.getName());
        removeAnnotations.add(DELETE.class.getName());
        //        removeAnnotations.add(Consumes.class.getName());
        //        removeAnnotations.add(Produces.class.getName());
        //        removeAnnotations.add(Api.class.getName());
        //        removeAnnotations.add(ApiOperation.class.getName());
        //        removeAnnotations.add(ApiResponses.class.getName());
        newRestEndpoint.annotations.removeAll(removeAnnotations);

        System.out.println(newRestEndpoint.toString());

        return newRestEndpoint;
    }

    private List<String> discoverAnnotations(final Method invokedMethod) {
        Class<?> type = invokedMethod.getDeclaringClass();
        final List<String> annotations = new ArrayList<>();
        while (type != null) {
            for (final Annotation annotation : type.getAnnotations()) {
                if (!annotations.contains(annotation.annotationType().getName())) {
                    annotations.add(annotation.annotationType().getName());
                }
            }
            type = type.getSuperclass();
        }
        for (final Annotation annotation : invokedMethod.getDeclaredAnnotations()) {
            if (!annotations.contains(annotation.annotationType().getName())) {
                annotations.add(annotation.annotationType().getName());
            }
        }
        return annotations;
    }

    private void discoverSwaggerDocInfos(final Method javaMethod, final Class<?> clazz,
            final RestEndpoint newRestEndpoint) {
        final ApiOperation apiOperations = javaMethod.getAnnotation(ApiOperation.class);
        if (apiOperations != null) {
            newRestEndpoint.description = apiOperations.value();
            newRestEndpoint.tags = Arrays.asList(apiOperations.tags());
            newRestEndpoint.notes = apiOperations.notes();
        }

        Consumes consume = javaMethod.getAnnotation(Consumes.class);
        if (consume == null) {
            consume = clazz.getAnnotation(Consumes.class);
        }
        if (consume != null) {
            newRestEndpoint.consumes = Arrays.asList(consume.value());
        }

        Produces produce = javaMethod.getAnnotation(Produces.class);
        if (produce == null) {
            produce = clazz.getAnnotation(Produces.class);
        }
        if (produce != null) {
            newRestEndpoint.produces = Arrays.asList(produce.value());
        }

        ApiResponses apiResponses = javaMethod.getAnnotation(ApiResponses.class);
        if (apiResponses != null && apiResponses.value() != null) {
            for (final ApiResponse apiRes : apiResponses.value()) {
                // Success Response Check
                if (apiRes.code() == 200) {
                    newRestEndpoint.successResponseTypes.add(apiRes.response().getName());
                }
                // Failure section
                else if (!newRestEndpoint.failureResponseTypes.contains(apiRes.response().getName())) {
                    newRestEndpoint.failureResponseTypes.add(apiRes.response().getName());
                }
            }
        }
    }

    /**
     * Get the parameters for the specified endpoint from the provided java method.
     */
    @SuppressWarnings("rawtypes")
    private void discoverParameters(Method method, RestEndpoint restEndpoint) {

        final Annotation[][] annotations = method.getParameterAnnotations();
        final Class[] parameterTypes = method.getParameterTypes();

        for (int i = 0; i < parameterTypes.length; i++) {
            final Class parameter = parameterTypes[i];

            // ignore parameters used to access context
            if ((parameter == Request.class) || (parameter == javax.servlet.http.HttpServletResponse.class) || (
                    parameter == javax.servlet.http.HttpServletRequest.class)) {
                continue;
            }

            final EndpointParameter nextParameter = new EndpointParameter();
            nextParameter.javaType = parameter.getName();

            final Annotation[] parameterAnnotations = annotations[i];
            for (final Annotation annotation : parameterAnnotations) {
                if (annotation instanceof PathParam) {
                    nextParameter.parameterType = ParameterType.PATH;
                    final PathParam pathparam = (PathParam) annotation;
                    nextParameter.name = pathparam.value();
                }
                else if (annotation instanceof QueryParam) {
                    nextParameter.parameterType = ParameterType.QUERY;
                    final QueryParam queryparam = (QueryParam) annotation;
                    nextParameter.name = queryparam.value();
                }
                else if (annotation instanceof DefaultValue) {
                    final DefaultValue defaultvalue = (DefaultValue) annotation;
                    nextParameter.defaultValue = defaultvalue.value();
                }
                // For Multi-Form payload
                else if (annotation instanceof FormDataParam) {
                    final FormDataParam formDataParam = (FormDataParam) annotation;
                    nextParameter.name = formDataParam.value();
                }
                else if (annotation instanceof Pattern) {
                    final Pattern pattern = (Pattern) annotation;
                    nextParameter.pattern = pattern.regexp();
                }
            }

            switch (nextParameter.parameterType) {
                case PATH:
                    restEndpoint.pathParameters.add(nextParameter);
                    break;
                case QUERY:
                    restEndpoint.queryParameters.add(nextParameter);
                    break;
                case PAYLOAD:
                    restEndpoint.payloadParameters.add(nextParameter);
                    break;
            }
        }
    }

    /**
     * Get the REST endpoint path for the specified class. This involves
     * (recursively) looking for @Parent annotations and getting the path for
     * that class before appending the location in the @Path annotation.
     */
    private String getRESTEndpointPath(Class<?> clazz) {
        String path = "";
        while (clazz != null) {
            final Annotation annotation = clazz.getAnnotation(Path.class);
            if (annotation != null) {
                path = ((Path) annotation).value() + path;
            }

            // TODO: FIX THIS
            //            Annotation parent = clazz.getAnnotation(Parent.class);
            //            if (parent != null) {
            //                clazz = ((Parent) parent).value();
            //            }
            //            else {
            //                clazz = null;
            //            }

            /*TEMP FIX*/
            clazz = null;
        }

        if (path.startsWith("/") == false) {
            path = "/" + path;
        }

        if (path.endsWith("/") == false) {
            path = path + "/";
        }

        // NOTE: Append the base URI
        return BASE_URI + path;
    }


    /**
     * Returns all of the classes in the specified package (including sub-packages).
     */
    @SuppressWarnings("rawtypes")
    private List<Class> getClasses(final String pkg) throws IOException, ClassNotFoundException {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        // turn package into the folder equivalent
        final String path = pkg.replace('.', '/');
        final Enumeration<URL> resources = classloader.getResources(path);
        final List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            final URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        final ArrayList<Class> classes = new ArrayList<Class>();
        for (final File directory : dirs) {
            classes.addAll(getClasses(directory, pkg));
        }
        return classes;
    }

    /**
     * Returns a list of all the classes from the package in the specified
     * directory. Calls itself recursively until no more directories are found.
     */
    @SuppressWarnings("rawtypes")
    private List<Class> getClasses(final File dir, final String pkg) throws ClassNotFoundException {
        final List<Class> classes = new ArrayList<Class>();
        if (!dir.exists()) {
            return classes;
        }
        final File[] files = dir.listFiles();
        for (final File file : files) {
            if (file.isDirectory()) {
                classes.addAll(getClasses(file, pkg + "." + file.getName()));
            }
            else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(pkg + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }

    public class RestEndpoint {
        private String uri;

        private MethodEnum method;

        private String javaClass;

        private String javaMethodName;

        private List<EndpointParameter> pathParameters = new ArrayList<EndpointParameter>();

        private List<EndpointParameter> queryParameters = new ArrayList<RESTEndpointsDocumenter.EndpointParameter>();

        private List<EndpointParameter> payloadParameters = new ArrayList<RESTEndpointsDocumenter.EndpointParameter>();

        private String javaMethodReturnType;

        private String description;

        private List<String> tags = new ArrayList<>();

        private String notes;

        private List<String> consumes = new ArrayList<>();

        private List<String> produces = new ArrayList<>();

        private List<String> successResponseTypes = new ArrayList<>();

        private List<String> failureResponseTypes = new ArrayList<>();

        private List<String> annotations = new ArrayList<>();

        // TODO: Enable this
        private List<String> requestHeaders;

        private List<String> responseHeaders;

        // TODO: Split the validation into diff data structure
        private List<String> payloadValidations;

        @Override
        public String toString() {
            return "RestEndpoint {" + "uri='" + uri + '\'' + ", method=" + method + '}';
        }
    }

    public class EndpointParameter {
        private ParameterType parameterType = ParameterType.PAYLOAD;

        private String javaType;

        private String defaultValue;

        private String name;

        private String pattern;
    }

}
