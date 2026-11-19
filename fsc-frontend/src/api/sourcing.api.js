import spoApi from './spoAxios.js';

// Fetch sourcing opportunities for the logged-in supplier
export const getInvitedSourcingOpportunities = async () => {
    const response = await spoApi.get(
        '/api/v1/spo/sourcing-requests/invited'
    );

    return response.data;


};

// Fetch bids for a particular sourcing request

export const getBidsForSourcingRequest = async (sourcingRequestId) => {

    const response = await spoApi.get(

        `/api/v1/spo/sourcing-requests/${sourcingRequestId}/bids`

    );

    return response.data;

};