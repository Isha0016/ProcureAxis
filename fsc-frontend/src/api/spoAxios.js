import axios from 'axios';

const spoApi = axios.create({
    baseURL: 'discerning-curiosity-production-ad1fdcd3.up.railway.app',
    headers: {
        'Content-Type': 'application/json'
    }
});

spoApi.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');

        if (token) {
            config.headers.token = token;
        }

        return config;
    },
    (error) => Promise.reject(error)
);

export default spoApi;