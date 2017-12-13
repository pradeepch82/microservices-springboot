package com.cassiomolin.example.shoppinglist.service;

import com.cassiomolin.example.shoppinglist.model.Product;
import com.cassiomolin.example.shoppinglist.model.ShoppingList;
import com.cassiomolin.example.shoppinglist.repository.ShoppingListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Optional;

/**
 * Service that provides operations for shopping lists.
 *
 * @author cassiomolin
 */
@Service
public class ShoppingListService {

    @Autowired
    private ProductApiClient productApiClient;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    public List<ShoppingList> getShoppingLists() {
        List<ShoppingList> shoppingLists = shoppingListRepository.findAll();
        shoppingLists.forEach(this::fillProductDetails);
        return shoppingLists;
    }

    public String createShoppingList(ShoppingList shoppingList) {
        shoppingList.getItems().forEach(product -> {
            if (!productApiClient.checkIfProductExists(product.getId())) {
                throw new ProductNotFoundException(String.format("Product not found with id %s", product.getId()));
            }
        });
        shoppingList = shoppingListRepository.save(shoppingList);
        return shoppingList.getId();
    }

    public ShoppingList getShoppingList(String id) {
        ShoppingList shoppingList = shoppingListRepository.findOne(id);
        if (shoppingList == null) {
            throw new NotFoundException();
        } else {
            fillProductDetails(shoppingList);
            return shoppingList;
        }
    }

    public void deleteShoppingList(String id) {
        ShoppingList shoppingList = shoppingListRepository.findOne(id);
        if (shoppingList == null) {
            throw new NotFoundException();
        } else {
            shoppingListRepository.delete(id);
        }
    }

    private void fillProductDetails(ShoppingList shoppingList) {
        shoppingList.getItems().forEach(item -> {
            Optional<Product> optionalProduct = productApiClient.getProduct(item.getId());
            optionalProduct.ifPresent(product -> item.setName(product.getName()));
        });
    }

    @StreamListener(ProductDeletedInput.PRODUCT_DELETED_INPUT)
    public void handleDeletedProduct(Product product) {
        shoppingListRepository.deleteProductsById(product.getId());
    }
}