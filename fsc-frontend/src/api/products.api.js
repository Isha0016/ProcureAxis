import api from './axios.js';

// Fetch products belonging to the logged-in supplier
export const getMyProducts = async () => {
    const response = await api.get('/api/v1/slo/products/my-products');
    return response.data;
};

// Fetch draft products belonging to the logged-in supplier
export const getMyDraftProducts = async () => {
    const response = await api.get('/api/v1/slo/products/my-drafts');
    return response.data;
};

// Fetch active product categories
export const getProductCategories = async () => {
    const response = await api.get('/api/v1/slo/product-categories');
    return response.data;
};

export const browseActiveProducts = async (page = 0, size = 50) => {
    const response = await api.get('/api/v1/slo/products/browse', {
        params: { page, size }
    });

    return response.data;
};