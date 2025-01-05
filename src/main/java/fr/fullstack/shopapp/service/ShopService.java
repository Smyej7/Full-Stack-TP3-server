package fr.fullstack.shopapp.service;

import fr.fullstack.shopapp.model.OpeningHoursShop;
import fr.fullstack.shopapp.model.Product;
import fr.fullstack.shopapp.model.Shop;
import fr.fullstack.shopapp.repository.elastic.ShopElasticRepository;
import fr.fullstack.shopapp.repository.jpa.ShopRepository;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShopService {
    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ShopRepository shopRepository;
    @Autowired
    private ShopElasticRepository shopElasticRepository;

    @Transactional
    public Shop createShop(Shop shop) throws Exception {
        validateOpeningHours(shop.getOpeningHours());
        try {
            Shop newShop = shopRepository.save(shop);
            // Refresh the entity after the save. Otherwise, @Formula does not work.
            em.flush();
            em.refresh(newShop);
            // Index the entity into idx_shops in ElasticSearch
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
            // delete nested relations with products
            deleteNestedRelations(shop);
            shopRepository.deleteById(id);
            shopElasticRepository.deleteById(id);
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
        // SORT
        if (sortBy.isPresent()) {
            switch (sortBy.get()) {
                case "name":
                    return shopRepository.findByOrderByNameAsc(pageable);
                case "createdAt":
                    return shopRepository.findByOrderByCreatedAtAsc(pageable);
                default:
                    return shopRepository.findByOrderByNbProductsAsc(pageable);
            }
        }

        // FILTERS AND SEARCH
        Page<Shop> shopList = getShopListWithFilter(name, inVacations, createdBefore, createdAfter, pageable);
        if (shopList != null) {
            return shopList;
        }


        // NONE
        return shopRepository.findByOrderByIdAsc(pageable);
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

    private void deleteNestedRelations(Shop shop) {
        List<Product> products = shop.getProducts();
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            product.setShop(null);
            em.merge(product);
            em.flush();
        }
    }

    private Shop getShop(Long id) throws Exception {
        Optional<Shop> shop = shopRepository.findById(id);
        if (!shop.isPresent()) {
            throw new Exception("Shop with id " + id + " not found");
        }
        return shop.get();
    }

    @Transactional
    public void syncExistingShops(Shop shop) throws Exception {
        Shop toIndex = shopRepository.save(shop);
        // Refresh the entity after the save. Otherwise, @Formula does not work.
        em.flush();
        em.refresh(toIndex);
        // Index the entity into idx_shops in ElasticSearch
        shopElasticRepository.save(toIndex);
    }

    private Page<Shop> getShopListWithFilter(
            Optional<String> name,
            Optional<Boolean> inVacations,
            Optional<String> createdAfter,
            Optional<String> createdBefore,
            Pageable pageable
    ) {

        if (name.isPresent()) {
            LocalDate after; LocalDate before;
            after = createdAfter.map(LocalDate::parse).orElse(LocalDate.EPOCH);
            before = createdBefore.map(LocalDate::parse).orElse(LocalDate.EPOCH.plusYears(90));
            if (inVacations.isEmpty()) {
                inVacations = Optional.of(false);
            }
            return shopElasticRepository.findAllByNameContainingAndCreatedAtAfterAndCreatedAtBeforeAndInVacationsEquals(name.get(), after, before, inVacations.get(), pageable);
//            return shopElasticRepository.findAllByNameContaining(name.get(), pageable);
        }
        if (inVacations.isPresent() && createdBefore.isPresent() && createdAfter.isPresent()) {
            return shopRepository.findByInVacationsAndCreatedAtGreaterThanAndCreatedAtLessThan(
                    inVacations.get(),
                    LocalDate.parse(createdAfter.get()),
                    LocalDate.parse(createdBefore.get()),
                    pageable
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

        if (inVacations.isPresent()) {
            return shopRepository.findByInVacations(inVacations.get(), pageable);
        }

        if (createdBefore.isPresent() && createdAfter.isPresent()) {
            return shopRepository.findByCreatedAtBetween(
                    LocalDate.parse(createdAfter.get()), LocalDate.parse(createdBefore.get()), pageable
            );
        }

        if (createdBefore.isPresent()) {
            return shopRepository.findByCreatedAtLessThan(
                    LocalDate.parse(createdBefore.get()), pageable
            );
        }

        return createdAfter.map(s -> shopRepository.findByCreatedAtGreaterThan(
                LocalDate.parse(s), pageable
        )).orElse(null);

    }

    private void validateOpeningHours(List<OpeningHoursShop> openingHours) {
        // Regrouper les horaires par jour
        Map<Long, List<OpeningHoursShop>> openingHoursByDay = openingHours.stream()
                .collect(Collectors.groupingBy(OpeningHoursShop::getDay));

        // Vérifier les chevauchements pour chaque jour
        openingHoursByDay.values().forEach(hours -> {
            try {
                checkForOverlap(hours);
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void checkForOverlap(List<OpeningHoursShop> dayOpeningHours) throws BadRequestException {
        // Trier les horaires par heure d'ouverture pour simplifier la vérification des chevauchements
        List<OpeningHoursShop> sortedHours = dayOpeningHours.stream()
                .sorted(Comparator.comparing(OpeningHoursShop::getOpenAt))
                .toList();

        // Vérifier uniquement les horaires consécutifs
        for (int i = 0; i < sortedHours.size() - 1; i++) {
            OpeningHoursShop current = sortedHours.get(i);
            OpeningHoursShop next = sortedHours.get(i + 1);

            if (isOverlapping(current, next)) {
                throw new BadRequestException(
                        String.format("Les horaires d'ouverture se chevauchent pour le jour %d : %s et %s",
                                current.getDay(), current, next)
                );
            }
        }
    }

    private boolean isOverlapping(OpeningHoursShop hours1, OpeningHoursShop hours2) {
        // Un chevauchement existe si l'heure de fermeture du premier dépasse ou touche l'heure d'ouverture du second
        return !hours1.getCloseAt().isBefore(hours2.getOpenAt());
    }

}