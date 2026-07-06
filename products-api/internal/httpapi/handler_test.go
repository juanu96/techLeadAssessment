package httpapi

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/juanu96/techLeadAssessment/products-api/internal/product"
)

func TestGetProductByID(t *testing.T) {
	request := httptest.NewRequest(http.MethodGet, "/products/PRD-001", nil)
	response := httptest.NewRecorder()

	NewHandler(product.NewCatalog()).ServeHTTP(response, request)

	if response.Code != http.StatusOK {
		t.Fatalf("expected status %d, got %d", http.StatusOK, response.Code)
	}

	var item product.Product
	if err := json.NewDecoder(response.Body).Decode(&item); err != nil {
		t.Fatalf("decode response: %v", err)
	}

	if item.ProductID != "PRD-001" {
		t.Fatalf("expected product PRD-001, got %s", item.ProductID)
	}

	if item.TaxCategory != product.TaxCategoryTaxed {
		t.Fatalf("expected tax category %s, got %s", product.TaxCategoryTaxed, item.TaxCategory)
	}
}

func TestGetUnknownProduct(t *testing.T) {
	request := httptest.NewRequest(http.MethodGet, "/products/PRD-999", nil)
	response := httptest.NewRecorder()

	NewHandler(product.NewCatalog()).ServeHTTP(response, request)

	if response.Code != http.StatusNotFound {
		t.Fatalf("expected status %d, got %d", http.StatusNotFound, response.Code)
	}

	assertError(t, response, "product not found")
}

func TestProductsEndpointRejectsUnsupportedMethods(t *testing.T) {
	request := httptest.NewRequest(http.MethodPost, "/products/PRD-001", nil)
	response := httptest.NewRecorder()

	NewHandler(product.NewCatalog()).ServeHTTP(response, request)

	if response.Code != http.StatusMethodNotAllowed {
		t.Fatalf("expected status %d, got %d", http.StatusMethodNotAllowed, response.Code)
	}

	if response.Header().Get("Allow") != http.MethodGet {
		t.Fatalf("expected Allow header to contain GET")
	}

	assertError(t, response, "method not allowed")
}

func TestHealthEndpoint(t *testing.T) {
	request := httptest.NewRequest(http.MethodGet, "/health", nil)
	response := httptest.NewRecorder()

	NewHandler(product.NewCatalog()).ServeHTTP(response, request)

	if response.Code != http.StatusOK {
		t.Fatalf("expected status %d, got %d", http.StatusOK, response.Code)
	}
}

func assertError(t *testing.T, response *httptest.ResponseRecorder, expected string) {
	t.Helper()

	var body map[string]string
	if err := json.NewDecoder(response.Body).Decode(&body); err != nil {
		t.Fatalf("decode error response: %v", err)
	}

	if body["error"] != expected {
		t.Fatalf("expected error %q, got %q", expected, body["error"])
	}
}
