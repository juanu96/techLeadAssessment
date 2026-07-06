package product

import "testing"

func TestCatalogContainsExpectedProducts(t *testing.T) {
	catalog := NewCatalog()
	if len(catalog.products) < 10 {
		t.Fatalf("expected at least 10 products, got %d", len(catalog.products))
	}

	tests := []struct {
		productID   string
		taxCategory TaxCategory
	}{
		{productID: "PRD-001", taxCategory: TaxCategoryTaxed},
		{productID: "PRD-004", taxCategory: TaxCategoryReduced},
		{productID: "PRD-007", taxCategory: TaxCategoryExempt},
		{productID: "PRD-008", taxCategory: TaxCategoryReduced},
		{productID: "PRD-010", taxCategory: TaxCategoryExempt},
	}

	for _, test := range tests {
		t.Run(test.productID, func(t *testing.T) {
			item, found := catalog.FindByID(test.productID)
			if !found {
				t.Fatalf("expected product %s to exist", test.productID)
			}

			if item.TaxCategory != test.taxCategory {
				t.Fatalf("expected tax category %s, got %s", test.taxCategory, item.TaxCategory)
			}
		})
	}
}

func TestCatalogReturnsFalseForUnknownProduct(t *testing.T) {
	catalog := NewCatalog()

	if _, found := catalog.FindByID("PRD-999"); found {
		t.Fatal("expected unknown product not to be found")
	}
}
