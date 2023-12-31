package com.example.demo;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.google.gson.GsonBuilder;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;

import jakarta.servlet.http.HttpServletRequest;

@SpringBootApplication
@RestController
public class DemoApplication {

  // This would be in a database.
  private Map<String, Map<String, Object>> products = getProducts();

  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

  // This would be in a database.
  private Map<String, Map<String, Object>> getProducts() {
    Map<String, Map<String, Object>> products = new HashMap<>() {
      {
        Map<String, Object> product1 = new HashMap<>();
        product1.put("description", "Product 1");
        product1.put("amount", 2000); // in pennies
        product1.put("currency", "usd");

        Map<String, Object> product2 = new HashMap<>();
        product2.put("description", "Product 2");
        product2.put("amount", 10000); // in pennies
        product2.put("currency", "usd");

        this.put("product_1", product1);
        this.put("product_2", product2);
      }
    };
    return products;
  }

  @PostMapping("/charge")
  @ResponseBody
  public String postCharge(@RequestBody Map<String, Object> body, HttpServletRequest request)
      throws StripeException {
    // Get the secret key from your stripe account.
    Stripe.apiKey = "sk_test_ZJCeWvsgPVxby9ScCVDon78M";

    // This is the token retrieved from stripe on the browser side.
    @SuppressWarnings("unchecked")
    final Map<String, Object> token = (Map<String, Object>) body.get("token");

    // This is any additional data we send from the browser.
    @SuppressWarnings("unchecked")
    final Map<String, Object> data = (Map<String, Object>) body.get("data");

    // Get the product by id from the database.
    String productId = (String) data.get("product");
    Map<String, Object> product = products.get(productId);

    // If the product is not found, return an error.
    if (product == null) {
      throw new ResponseStatusException(HttpStatusCode.valueOf(400), "Product not found");
    }

    // Add the product to data to be stored as metadata.
    data.putAll(product);

    // Add the token and product information to the charge parameters.
    Map<String, Object> params = new HashMap<>();
    params.put("source", token.get("id"));
    params.put("amount", product.get("amount"));
    params.put("currency", product.get("currency"));
    params.put("description", product.get("description"));
    params.put("metadata", data);

    // Create the charge.
    Charge charge = Charge.create(params);
    return new GsonBuilder().setPrettyPrinting().create().toJson(charge);
  }
}
