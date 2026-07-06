package product

type TaxCategory string

const (
	TaxCategoryTaxed   TaxCategory = "GRAVADO"
	TaxCategoryReduced TaxCategory = "REDUCIDO"
	TaxCategoryExempt  TaxCategory = "EXENTO"
)

type Product struct {
	ProductID     string      `json:"productId"`
	Name          string      `json:"name"`
	SKU           string      `json:"sku"`
	Category      string      `json:"category"`
	TaxCategory   TaxCategory `json:"taxCategory"`
	UnitOfMeasure string      `json:"unitOfMeasure"`
}
