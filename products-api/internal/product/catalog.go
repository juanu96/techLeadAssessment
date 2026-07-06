package product

type Catalog struct {
	products map[string]Product
}

func NewCatalog() *Catalog {
	products := []Product{
		{ProductID: "PRD-001", Name: "Gaseosa 600ml", SKU: "GAS-600-PET", Category: "Bebidas", TaxCategory: TaxCategoryTaxed, UnitOfMeasure: "UNIDAD"},
		{ProductID: "PRD-002", Name: "Chocolate con leche 50g", SKU: "CHO-050-UND", Category: "Confitería", TaxCategory: TaxCategoryTaxed, UnitOfMeasure: "UNIDAD"},
		{ProductID: "PRD-003", Name: "Detergente en polvo 1kg", SKU: "DET-1000-BOL", Category: "Aseo", TaxCategory: TaxCategoryTaxed, UnitOfMeasure: "BOLSA"},
		{ProductID: "PRD-004", Name: "Arroz blanco 1kg", SKU: "ARR-1000-BOL", Category: "Alimentos básicos", TaxCategory: TaxCategoryReduced, UnitOfMeasure: "BOLSA"},
		{ProductID: "PRD-005", Name: "Frijol rojo 1kg", SKU: "FRI-1000-BOL", Category: "Alimentos básicos", TaxCategory: TaxCategoryReduced, UnitOfMeasure: "BOLSA"},
		{ProductID: "PRD-006", Name: "Semilla de maíz 5kg", SKU: "SEM-MAIZ-5K", Category: "Insumos agrícolas", TaxCategory: TaxCategoryReduced, UnitOfMeasure: "SACO"},
		{ProductID: "PRD-007", Name: "Agua potable 600ml", SKU: "AGU-600-PET", Category: "Bebidas", TaxCategory: TaxCategoryExempt, UnitOfMeasure: "UNIDAD"},
		{ProductID: "PRD-008", Name: "Aceite vegetal 1L", SKU: "ACE-1000-BOT", Category: "Alimentos básicos", TaxCategory: TaxCategoryReduced, UnitOfMeasure: "BOTELLA"},
		{ProductID: "PRD-009", Name: "Acetaminofén 500mg", SKU: "MED-ACE-500", Category: "Medicamentos", TaxCategory: TaxCategoryExempt, UnitOfMeasure: "CAJA"},
		{ProductID: "PRD-010", Name: "Leche entera 1L", SKU: "LEC-1000-CAJ", Category: "Canasta básica", TaxCategory: TaxCategoryExempt, UnitOfMeasure: "CAJA"},
	}

	byID := make(map[string]Product, len(products))
	for _, item := range products {
		byID[item.ProductID] = item
	}

	return &Catalog{products: byID}
}

func (c *Catalog) FindByID(productID string) (Product, bool) {
	item, found := c.products[productID]
	return item, found
}
