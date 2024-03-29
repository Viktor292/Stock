import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


public class StaticResourceProcessor implements Processor {

  static Crypto cr = new Crypto();

  private StockServiceJDBC stockServiceJDBC = new StockServiceJDBC();
  private static HashMap<String, String> tokensUsers = new HashMap();
  private static HashMap<String, String> loginsAndPasswords = new HashMap<>();
  private static final SecureRandom secureRandom = new SecureRandom(); //threadsafe
  private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder(); //threadsafe

  static byte[] key = "MZygpewJsCpRrfOr".getBytes(StandardCharsets.UTF_8);

  public StaticResourceProcessor() throws SQLException, ClassNotFoundException {
    loginsAndPasswords.put("admin", Hash.md5("1234"));
  }

  public static String tokenGen() {
    byte[] randomBytes = new byte[24];
    secureRandom.nextBytes(randomBytes);
    return base64Encoder.encodeToString(randomBytes).substring(1, 11);
  }

  public boolean isNumber(String str) {
    if (str == null || str.isEmpty()) return false;
    for (int i = 0; i < str.length(); i++) {
      if (!Character.isDigit(str.charAt(i))) return false;
    }
    return true;
  }

  @Override
  public void process(Request request, Response response) throws IOException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException, InvalidKeySpecException, InvalidAlgorithmParameterException {
    try {

      if(request.getType().equals("GET") && request.getURI().equals("/login")) {
        response.sendText(login(request.getParameter("Login").substring(1, 6), request.getParameter("Password").substring(1, 33)));
        return;
      }
      if(!tokensUsers.containsValue(request.getParameter("Token").substring(1, 11))) {
        response.sendText("403");
      } else {
//        System.out.println(request.getBody().substring(0, request.getBody().indexOf("}")));
        if (request.getType().equals("POST") && request.getURI().equals("/api/good")) {
          response.sendText(post(request.getBody()));
        } else if (request.getType().equals("POST") && request.getURI().equals("/api/table")) {
          response.sendText(postTable(request.getBody()));
        }else if (request.getType().equals("GET") && request.getURI().substring(0, request.getURI().lastIndexOf("/")).equals("/api/good")) {
          if(request.getURI().substring(request.getURI().lastIndexOf("/") + 1).contains("naming:")) {
              response.sendText(getName(request.getURI().substring(request.getURI().lastIndexOf("/") + 8)));
          }
          else response.sendText(getAllTable(request.getURI().substring(request.getURI().lastIndexOf("/") + 1)));
        } else if(request.getType().equals("GET") && request.getURI().equals("/api/all")) {
          response.sendText(getAll());
        } else if(request.getType().equals("GET") && request.getURI().equals("/api/tables")) {
          response.sendText(getAllTables());
        } else if(request.getType().equals("GET")) {
          response.sendText(get(request.getURI().substring(5, request.getURI().lastIndexOf("/"))   , request.getURI().substring(request.getURI().lastIndexOf("/") + 1)));
        } else if (request.getType().equals("DELETE")) {
          if(request.getURI().contains("api/table")){
            String group = request.getURI().substring(request.getURI().indexOf("table/") + 6);
            response.sendText(deleteTable(group));
          } else if(request.getURI().contains("api/good")){
            if(request.getURI().substring(request.getURI().indexOf("good/") + 5).contains("naming:")) {
                String naming = request.getURI().substring(request.getURI().lastIndexOf("/") + 8);
                response.sendText(deleteName(naming));
            }else {
                String group = request.getURI().substring(request.getURI().indexOf("good/") + 5, request.getURI().lastIndexOf("/"));
                String id = request.getURI().substring(request.getURI().lastIndexOf("/") + 1);
                response.sendText(delete(group, id));
            }
          }
        } else if (request.getType().equals("PUT") && request.getURI().equals("/api/good")) {
          response.sendText(put(request.getBody()));
        }  else if (request.getType().equals("PUT") && request.getURI().equals("/api/table")) {
          response.sendText(putTable(request.getBody()));
        }
      }

    } catch (ParseException | SQLException e) {
      e.printStackTrace();
    }
  }

  public String post(String jsonStr) throws ParseException, SQLException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, ClassNotFoundException, InvalidKeySpecException, InvalidAlgorithmParameterException {
        jsonStr = (cr.decrypt("345354345", jsonStr));
        JSONParser parser = new JSONParser();
        System.out.println(jsonStr);
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) parser.parse(jsonStr.toString());
        } catch (Exception e) {
            jsonObject = (JSONObject) parser.parse(jsonStr.toString());
        }
        try {
            //    public void updateItem(String tableName, int index, String column, String value){
            String coll = (String)jsonObject.get("field");
            if(jsonObject.keySet().contains("id")) {
              if(get(String.valueOf(jsonObject.get("group")), String.valueOf(jsonObject.get("id"))).equals("404 Not Found")) return "404 Not Found";
              try {
                stockServiceJDBC.updateItem(
                        (String) jsonObject.get("group"),
                        (long) jsonObject.get("id"),
                        coll,
                        (String) jsonObject.get(coll));
              } catch (java.lang.ClassCastException e) {
                stockServiceJDBC.updateItem(
                        (String) jsonObject.get("group"),
                        (long) jsonObject.get("id"),
                        coll,
                        (long) jsonObject.get(coll));
              }
            } else {
                String naming;
                try {
                    naming = (String) jsonObject.get("_naming");
                } catch (Exception e) {
                    naming = (String) jsonObject.get("naming");
                }
              if(getName(naming).equals("404 Not Found")) return "404 Not Found";
              try {

                stockServiceJDBC.updateItemName(
                        getNameGroup(naming),
                        naming,
                        coll,
                        (String) jsonObject.get(coll));
              } catch (java.lang.ClassCastException e) {
                stockServiceJDBC.updateItemName(
                        getNameGroup(naming),
                        naming,
                        coll,
                        (long) jsonObject.get(coll));
              }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "409 Conflict";
        }
        return "204 No Content";
    }


  public String postTable(String jsonStr) throws ParseException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, ClassNotFoundException, InvalidKeySpecException, InvalidAlgorithmParameterException {
    jsonStr = (cr.decrypt("345354345", jsonStr));
    JSONParser parser = new JSONParser();
    System.out.println(jsonStr);
    JSONObject jsonObject = null;
    try {
      jsonObject = (JSONObject) parser.parse(jsonStr.toString());
    } catch (Exception e) {
      jsonObject = (JSONObject) parser.parse(jsonStr.toString());
    }
    if (!stockServiceJDBC.getAllTables().contains(jsonObject.get("namingOld"))) return "404 Not Found";
    try {
      stockServiceJDBC.renameTable((String) jsonObject.get("namingOld"), (String)jsonObject.get("namingNew"));
    } catch (Exception e) {
      e.printStackTrace();
      return "409 Conflict";
    }
    return "204 No Content";

  }

  public String put(String jsonStr) throws ParseException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, ClassNotFoundException, InvalidKeySpecException, InvalidAlgorithmParameterException, SQLException {
    jsonStr = (cr.decrypt("345354345", jsonStr));
    JSONParser parser = new JSONParser();
    JSONObject jsonObject = null;
    jsonObject = (JSONObject) parser.parse(jsonStr);
    if((long)jsonObject.get("quantity") < 0 || !getName((String) jsonObject.get("naming")).contains("404 Not Found")) {
      return "409 Conflict";
    }
    try {
      stockServiceJDBC.insertProduct((String)jsonObject.get("group"), (String) jsonObject.get("naming"), (String) jsonObject.get("description"), (String) jsonObject.get("manufacturer"), (long) jsonObject.get("price"), (long) jsonObject.get("quantity"));
    }catch (Exception e) {
      try {
        stockServiceJDBC.createTable((String) jsonObject.get("group"));
        stockServiceJDBC.insertProduct((String)jsonObject.get("group"), (String) jsonObject.get("naming"), (String) jsonObject.get("description"), (String) jsonObject.get("manufacturer"), (long) jsonObject.get("price"), (long)jsonObject.get("quantity"));
      } catch (Exception e2) {
        return "409 Conflict";
      }
    }
    return "201 Created";
  }

  public String putTable(String jsonStr) throws ParseException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, ClassNotFoundException, InvalidKeySpecException, InvalidAlgorithmParameterException, SQLException {
    jsonStr = (cr.decrypt("345354345", jsonStr));
    JSONParser parser = new JSONParser();
    JSONObject jsonObject = null;
    jsonObject = (JSONObject) parser.parse(jsonStr);
    String nameGroup = (String)jsonObject.get("naming");
    if(getAllTables().contains(nameGroup)) {
      return "409 Conflict";
    }
    try {
      stockServiceJDBC.createTable(nameGroup);
    }catch (Exception e) {
      return "409 Conflict";
    }
    return "201 Created";
  }

  public String get(String group, String id) {
    JSONObject jsonObject = new JSONObject();
    try{
      ResultSet resultSet = stockServiceJDBC.readItemByIndex(group, Integer.parseInt(id));
      resultSet.first();
      jsonObject.put("naming", resultSet.getString("naming"));
      jsonObject.put("quantity", resultSet.getString("quantity"));
      jsonObject.put("description", resultSet.getString("description"));
      jsonObject.put("manufacturer", resultSet.getString("manufacturer"));
      jsonObject.put("id", resultSet.getString("id"));
      jsonObject.put("price", resultSet.getString("price"));
    } catch (Exception e) {
      e.printStackTrace();
      return "404 Not Found";
    }
    return  jsonObject.toString();
  }
//readItemByName
/*
  public String getName(String naming) {
    JSONObject jsonObject = new JSONObject();
    try{
      ResultSet resultSet = stockServiceJDBC.getProductName(naming);
      resultSet.first();
      resultSet.next();
      jsonObject.put("naming", resultSet.getString("naming"));
      jsonObject.put("quantity", resultSet.getString("quantity"));
      jsonObject.put("description", resultSet.getString("description"));
      jsonObject.put("manufacturer", resultSet.getString("manufacturer"));
      jsonObject.put("id", resultSet.getString("id"));
      jsonObject.put("price", resultSet.getString("price"));
    } catch (Exception e) {
      e.printStackTrace();
      return "404 Not Found";
    }
    System.out.println("200 Ok");
    return  jsonObject.toString();
  }
*/

  public String getName(String naming) {
    JSONObject jsonObject = new JSONObject();
    try{
      for(String table : stockServiceJDBC.getAllTables()) {
        ResultSet resultSet = stockServiceJDBC.readItemByName(table, naming);
        resultSet.first();
        try {
          jsonObject.put("naming", resultSet.getString("naming"));
          jsonObject.put("quantity", resultSet.getString("quantity"));
          jsonObject.put("description", resultSet.getString("description"));
          jsonObject.put("manufacturer", resultSet.getString("manufacturer"));
          jsonObject.put("id", resultSet.getString("id"));
          jsonObject.put("price", resultSet.getString("price"));
        } catch (java.sql.SQLException e) {
          continue;
        }
        return  jsonObject.toString();
      }
    } catch (Exception e) {
      e.printStackTrace();
      return "404 Not Found";
    }
    System.out.println("200 Ok");
    return "404 Not Found";
  }

  public String getNameGroup(String naming) {
    JSONObject jsonObject = new JSONObject();
    try{
      for(String table : stockServiceJDBC.getAllTables()) {
        ResultSet resultSet = stockServiceJDBC.readItemByName(table, naming);
        resultSet.first();
        try {
          jsonObject.put("naming", resultSet.getString("naming"));
          jsonObject.put("quantity", resultSet.getString("quantity"));
          jsonObject.put("description", resultSet.getString("description"));
          jsonObject.put("manufacturer", resultSet.getString("manufacturer"));
          jsonObject.put("id", resultSet.getString("id"));
          jsonObject.put("price", resultSet.getString("price"));
        } catch (java.sql.SQLException e) {
          continue;
        }
        return table;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return "404 Not Found";
    }
    System.out.println("200 Ok");
    return "404 Not Found";
  }

  public String getAll() {
    JSONObject jsonObject = new JSONObject();
    String allDb = "";
    ArrayList<String> tables = stockServiceJDBC.getAllTables();
    for(String table : tables) {
      try {
        ResultSet resultSet = stockServiceJDBC.readTable(table);
        while (resultSet.next()) {
          jsonObject = new JSONObject();
          jsonObject.put("naming", resultSet.getString("naming"));
          jsonObject.put("quantity", resultSet.getString("quantity"));
          jsonObject.put("description", resultSet.getString("description"));
          jsonObject.put("manufacturer", resultSet.getString("manufacturer"));
          jsonObject.put("id", resultSet.getString("id"));
          jsonObject.put("price", resultSet.getString("price"));
          allDb += jsonObject.toString() + "\n";
          System.out.println(jsonObject.toString());
        }
      } catch (SQLException e) {
        e.printStackTrace();
        return "404 Not Found";
      }
    }
    System.out.println("200 Ok");
    return  allDb;
  }

  public String getAllTables() {
    String allTb = "";
    ArrayList<String> tables = stockServiceJDBC.getAllTables();
    for(String table : tables) {
      allTb += table + "\n";
    }
    System.out.println("200 Ok");
    return  allTb;
  }
/*
  public String getTable(String table) {
    JSONObject jsonObject = new JSONObject();
    try{
      ResultSet resultSet = stockServiceJDBC.getProductId(Integer.parseInt(id));
      resultSet.first();
      resultSet.next();
      jsonObject.put("naming", resultSet.getString("naming"));
      jsonObject.put("numberOfProduct",resultSet.getString("numberOfProduct"));
      jsonObject.put("description", resultSet.getString("description"));
      jsonObject.put("manufacturer",resultSet.getString("manufacturer"));
      jsonObject.put("id",resultSet.getString("id"));
    } catch (SQLException e) {
      e.printStackTrace();
      return "404 Not Found";
    }
    return "200 Ok " + jsonObject.toString();
  }*/

  public String getAllTable(String table) {
    JSONObject jsonObject = new JSONObject();
    String allDb = "";
    try{
      ResultSet resultSet = stockServiceJDBC.readTable(table);
      while (resultSet.next()) {
        jsonObject = new JSONObject();
        jsonObject.put("naming", resultSet.getString("naming"));
        jsonObject.put("quantity", resultSet.getString("quantity"));
        jsonObject.put("description", resultSet.getString("description"));
        jsonObject.put("manufacturer", resultSet.getString("manufacturer"));
        jsonObject.put("id", resultSet.getString("id"));
        jsonObject.put("price", resultSet.getString("price"));
        allDb += jsonObject.toString() + "\n";
        System.out.println(jsonObject.toString());
      }
    } catch (SQLException e) {
      return "404 Not Found";
    }
    System.out.println("200 Ok");
    return   allDb;
  }

  public String login(String login, String password) {
    System.out.println(login);
    System.out.println(loginsAndPasswords.size() + " PASSWORD1: " + "admin" +
            " PASSWORD2: " + login + "***: " + loginsAndPasswords.containsKey(login));
    System.out.println(login.equalsIgnoreCase("admin"));
    if(loginsAndPasswords.containsKey(login) && loginsAndPasswords.get(login).equals(password)) {
      String token = tokenGen();
      tokensUsers.put(login, token);
      return token;
    }
    return "401 Unauthorized";
  }

  public String delete(String table, String id) throws SQLException {
    try{
      stockServiceJDBC.deleteProductId(table, Integer.parseInt(id));
    } catch (Exception e) {
      e.printStackTrace();
      return "404 Not Found";
    }
    return "204 No Content";
  }

    public String deleteName(String naming) {
        try{
            stockServiceJDBC.deleteProduct(getNameGroup(naming), naming);
        } catch (Exception e) {
            e.printStackTrace();
            return "404 Not Found";
        }
        return "204 No Content";
    }

  public String deleteTable(String table) throws SQLException {
    try{
      stockServiceJDBC.dropTable(table);
    } catch (Exception e) {
      e.printStackTrace();
      return "404 Not Found";
    }
    return "204 No Content";
  }
}