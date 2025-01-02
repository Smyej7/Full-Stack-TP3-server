package fr.fullstack.shopapp.service;

import fr.fullstack.shopapp.model.OpeningHoursShop;
import fr.fullstack.shopapp.model.Product;
import fr.fullstack.shopapp.model.Shop;
import fr.fullstack.shopapp.repository.elastic.ShopElasticRepository;
import fr.fullstack.shopapp.repository.jpa.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShopService {

    // Dependencies
    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ShopElasticRepository shopElasticRepository;

    // Public API

    @Transactional
    public Shop createShop(Shop shop) throws Exception {
        checkForOverlap(shop.getOpeningHours());
        try {
            Shop newShop = shopRepository.save(shop);
            em.flush();
            em.refresh(newShop);
            shopElasticRepository.save(newShop);
            return newShop;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    @Transactional
    public void deleteShopById(long id) throws Exception {
        try {
            Shop shop = getShop(id);
            deleteNestedRelations(shop);
            shopRepository.deleteById(id);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    public Shop getShopById(long id) throws Exception {
        try {
            return getShop(id);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    public Page<Shop> getShopList(
            Optional<String> name,
            Optional<String> sortBy,
            Optional<Boolean> inVacations,
            Optional<String> createdBefore,
            Optional<String> createdAfter,
            Pageable pageable
    ) {
        if (sortBy.isPresent()) {
            return getSortedShops(sortBy.get(), pageable);
        }
        Page<Shop> filteredShops = getShopListWithFilter(name, inVacations, createdBefore, createdAfter, pageable);
        return filteredShops != null ? filteredShops : shopRepository.findByOrderByIdAsc(pageable);
    }

    @Transactional
    public Shop updateShop(Shop shop) throws Exception {
        try {
            getShop(shop.getId());
            return this.createShop(shop);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    // Private Utility Methods

    private void deleteNestedRelations(Shop shop) {
        List<Product> products = shop.getProducts();
        for (Product product : products) {
            product.setShop(null);
            em.merge(product);
        }
        em.flush();
    }

    private Shop getShop(Long id) throws Exception {
        return shopRepository.findById(id)
                .orElseThrow(() -> new Exception("Shop with id " + id + " not found"));
    }

    private Page<Shop> getSortedShops(String sortBy, Pageable pageable) {
        switch (sortBy) {
            case "name":
                return shopRepository.findByOrderByNameAsc(pageable);
            case "createdAt":
                return shopRepository.findByOrderByCreatedAtAsc(pageable);
            default:
                return shopRepository.findByOrderByNbProductsAsc(pageable);
        }
    }

    private Page<Shop> getShopListWithFilter(
            Optional<String> name,
            Optional<Boolean> inVacations,
            Optional<String> createdAfter,
            Optional<String> createdBefore,
            Pageable pageable
    ) {
        if (name.isPresent()) {
            LocalDate after = createdAfter.map(LocalDate::parse).orElse(LocalDate.EPOCH);
            LocalDate before = createdBefore.map(LocalDate::parse).orElse(LocalDate.EPOCH.plusYears(90));
            boolean inVacationState = inVacations.orElse(false);
            return shopElasticRepository.findAllByNameContainingAndCreatedAtAfterAndCreatedAtBeforeAndInVacationsEquals(
                    name.get(), after, before, inVacationState, pageable
            );
        }

        return getFilteredShopsByCriteria(inVacations, createdAfter, createdBefore, pageable);
    }

    private Page<Shop> getFilteredShopsByCriteria(
            Optional<Boolean> inVacations,
            Optional<String> createdAfter,
            Optional<String> createdBefore,
            Pageable pageable
    ) {
        if (inVacations.isPresent() && createdBefore.isPresent() && createdAfter.isPresent()) {
            return shopRepository.findByInVacationsAndCreatedAtGreaterThanAndCreatedAtLessThan(
                    inVacations.get(), LocalDate.parse(createdAfter.get()), LocalDate.parse(createdBefore.get()), pageable
            );
        }

        if (inVacations.isPresent() && createdBefore.isPresent()) {
            return shopRepository.findByInVacationsAndCreatedAtLessThan(
                    inVacations.get(), LocalDate.parse(createdBefore.get()), pageable
            );
        }

        if (inVacations.isPresent() && createdAfter.isPresent()) {
            return shopRepository.findByInVacationsAndCreatedAtGreaterThan(
                    inVacations.get(), LocalDate.parse(createdAfter.get()), pageable
            );
        }

        if (createdBefore.isPresent() && createdAfter.isPresent()) {
            return shopRepository.findByCreatedAtBetween(
                    LocalDate.parse(createdAfter.get()), LocalDate.parse(createdBefore.get()), pageable
            );
        }

        return createdAfter.map(after -> shopRepository.findByCreatedAtGreaterThan(
                LocalDate.parse(after), pageable
        )).orElse(null);
    }

    // Validation Methods

    private void validateOpeningHours(List<OpeningHoursShop> openingHours) {
        Map<Long, List<OpeningHoursShop>> openingHoursByDay = openingHours.stream()
                .collect(Collectors.groupingBy(OpeningHoursShop::getDay));

        openingHoursByDay.values().forEach(this::checkForOverlap);
    }

    private void checkForOverlap(List<OpeningHoursShop> dayOpeningHours) {
        List<OpeningHoursShop> sortedHours = dayOpeningHours.stream()
                .sorted(Comparator.comparing(OpeningHoursShop::getOpenAt))
                .toList();

        for (int i = 0; i < sortedHours.size() - 1; i++) {
            OpeningHoursShop current = sortedHours.get(i);
            OpeningHoursShop next = sortedHours.get(i + 1);

            if (isOverlapping(current, next)) {
                throw new IllegalArgumentException(
                        String.format("Overlapping hours on day %d: %s and %s", current.getDay(), current, next)
                );
            }
        }
    }

    private boolean isOverlapping(OpeningHoursShop hours1, OpeningHoursShop hours2) {
        return !hours1.getCloseAt().isBefore(hours2.getOpenAt());
    }
}
