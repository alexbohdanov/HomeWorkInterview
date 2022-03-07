import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InterviewQuestion {

    public enum Product{
        PRODUCTA("ProDuctVentX", new BigDecimal("10")),
        PRODUCTB("HEV_Crowbar", new BigDecimal("35.7"));
        private String name;
        private BigDecimal cost;
        private Product(String name, BigDecimal cost) {
            this.name = name;
            this.cost = cost;
        }
        public String getName() {
            return this.name;
        }
        public BigDecimal getCost() {
            return this.cost;
        }
        private static Product getProduct(String name) {
            Product productValue = null;
            for (Product product : Product.values()) {
                if (product.getName().equals(name)) {
                    productValue = product;
                }
            }
            return productValue;
        }
    }

    private enum User{
        BOB("Bob", 		false, 	new BigDecimal("2.35")),
        DALE("Dale", 	false, 	new BigDecimal("0.22")),
        LAURA("Laura", 	false, 	BigDecimal.ONE),
        DIANE("Diane", 	true, 	BigDecimal.ZERO);
        //...
        private String name;
        private boolean admin;
        private BigDecimal discount;
        private User(String name, boolean admin, BigDecimal discount) {
            this.name = name;
            this.admin = admin;
            this.discount = discount;
        }
        public String getName() {
            return this.name;
        }
        public boolean isAdmin() {
            return this.admin;
        }
        public BigDecimal getDiscount() {
            return this.discount;
        }
        public static User getUser(String name) {
            User userValue = null;
            for (User user : User.values()) {
                if (user.getName().equals(name)) {
                    userValue = user;
                }
            }
            return userValue;
        }
    }

    private static final int MAX_LOGINS = 3;
    private static final Map<String, User> SESSIONS = new HashMap<>();
    private static final Map<User, List<Product>> CARTS = new HashMap<>();
    private static final Set<String> FUNCTION_CONTENT_EXCEPTIONS = new HashSet<>();
    private static boolean gst = false;
    static {
        FUNCTION_CONTENT_EXCEPTIONS.add("userLogout");
        FUNCTION_CONTENT_EXCEPTIONS.add("getCartDetails");
        FUNCTION_CONTENT_EXCEPTIONS.add("clearCart");
    }

    private InterviewQuestion() {
        // No instances here, only static hell.
    }

    public static Map<String, Object> queryServer(String contextID, String function, Map<String, Object> content){

        Map<String, Object> response = new HashMap<>();
        if (function != null) {
            try {

                if (content == null && !FUNCTION_CONTENT_EXCEPTIONS.contains(function)) {
                    throw new NullPointerException("No content provided");
                }

                //UserLogin being exceptional
                if ((contextID == null || !SESSIONS.containsKey(contextID)) && !"userLogin".equals(function)) {
                    throw new NullPointerException("Invalid context ID provided.");
                }

                switch (function) {
                    case "userLogout" :
                        SESSIONS.remove(contextID);
                        break;
                    case "userLogin":
                        response.put("contextID", login(String.valueOf(content.get("userName"))));
                        break;
                    case "adminSettings":
                        adminSettings(contextID, content.get("gst"));
                        break;
                    case "addProductToCart":
                        addProductToCart(contextID, String.valueOf(content.get("product")));
                        break;
                    case "clearCart":
                        CARTS.get(SESSIONS.get(contextID)).clear();
                        break;
                    case "getCartDetails":
                        response.putAll(getCartDetails(contextID));
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown function provided.");
                }

                //Because of all the crazy exception throwing, anything that gets here should theoretically be true... probably.
                response.put("success", true);


            } catch(Exception ex) {
                response.put("errorMessage", ex.getMessage());
                response.put("errorType", ex.getClass().toString());
                response.put("success", Boolean.FALSE);
            }
        }

        return response;
    }

    private static String login(String name) {
        final String contextID;
        User user;
        if ((user = User.getUser(name)) != null) {
            if (SESSIONS.size() >= MAX_LOGINS) {
                throw new IllegalStateException("Max logins reached... logout or clear sessions first.");
            } else {
                contextID = UUID.randomUUID().toString();
                SESSIONS.put(contextID, user);
                if (CARTS.get(user) == null) {
                    CARTS.put(user, new ArrayList<Product>());
                }
            }
        } else {
            throw new IllegalArgumentException("User doesn't exist!");
        }

        return contextID;
    }

    private static void adminSettings(String contextID, Object requestGST) {
        if (requestGST instanceof Boolean) {
            if (SESSIONS.get(contextID).isAdmin()) {
                if (Boolean.TRUE.equals(requestGST)) {
                    gst = true;
                } else {
                    gst = false;
                }
            } else {
                throw new IllegalStateException("This user is not an Admin!");
            }
        } else {
            throw new IllegalArgumentException("Boolean GST value has not been provided.");
        }
    }

    private static void addProductToCart(String contextID, String productName) {
        Product product;
        if ((product = Product.getProduct(productName)) != null) {
            CARTS.get(SESSIONS.get(contextID)).add(product);
        } else {
            throw new IllegalArgumentException("Product String value has not been provided.");
        }
    }

    private static Map<String, Object> getCartDetails(String contextID) {
        Map<String, Object> cartContents = new HashMap<>();

        User user = SESSIONS.get(contextID);
        List<Product> cart = CARTS.get(user);

        cartContents.put("products", cart);
        cartContents.put("totalQty", (Integer) cart.size());

        BigDecimal totalCost = BigDecimal.ZERO;
        for (Product product: cart) {
            totalCost = totalCost.add(product.getCost().subtract(user.getDiscount()));
        }

        if (gst) {
            totalCost = totalCost.multiply(new BigDecimal("1.1"));
        }

        cartContents.put("totalCost", totalCost);

        return cartContents;
    }

}