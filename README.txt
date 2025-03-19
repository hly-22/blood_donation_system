sID: s3979290
name: Ly Minh Hanh

I. Functionalities
    a. User Registration and Authentication
	- Firebase Authentication for email/password method and Google Sign-In.
	- Firestore integration to retrieve and manage user-specific data, such as user type (e.g., donor, site manager, or super user).
	- User registration flow where users can sign up and log in.
	- Account creation for proxy donors (donors registering on behalf of others).
    b. For Site Managers
	- Add and edit donation sites by selecting a location on the map view. The manager can input site details such as hours of operation and blood types needed. The address is generated from the coordinates of the selected location.
	- View registered donors at their sites.
	- Input donation data of registered donors as well as view their donation history.
	- Add and view volunteers at their sites (add/remove volunteers).
	- Update donation site details as necessary.
    c. For Donors
        - View nearby sites based on their location and camera position
        - Select a site to view information of the site
        - Filter shown sites by required blood types
        - Register for themselves to donate at a site, which would cause the marker to change color reflecting the change
        - Register for other to donate at a site which includes creating a new donor (proxy donor)
    d. For Super user
        - Display all sites available on a map view
        - View donation outcomes at a site by selecting the marker
        - Generate PDFs that are saved in the Downloads folder of the device



II. Technology used
    - Firebase Authentication: email/password login method and Google Sign-in
    - Firebase Firestore: database of application
    - Google Maps Platform: for displaying and manipulating the map view and the locations
    - Directions API (Google Maps Platform): to get routes from a location to another location
    - Glide: for loading and styling images 

III. Limitations and Drawbacks
    a. Missing Requirements
        Although the application is fundamentally functional, there are several features and requirements that could not be implemented due to time and knowledge constraints:
            -  Push Notifications for Donors: Currently, there are no push notifications to inform donors when site managers update the information for the donation sites they are registered with. This functionality could be implemented using Firebase Cloud Messaging (FCM) and would be included in future versions if time allowed.
            - Super User CRUD Operations: The super user functionality currently lacks full CRUD operations (Create, Read, Update, Delete) for managing users within the application. This is an important feature that would enhance the super user's ability to manage donors, site managers, and volunteers.
            - Limited Filtering Options: While some basic filters are available, the application could benefit from more comprehensive filtering options to improve the user experience. Features such as filtering by donation history, blood type, or other criteria could provide a more intuitive and user-friendly interface.
        These limitations are acknowledged, and future iterations of the app would address these missing features to provide a more complete and refined experience.
    b. Limitations
        To keep the application simple and focused, several features and considerations were not fully explored during development:
            - Limited Authentication Methods: Currently, the application supports only two authentication methods: Google sign-in and email/password sign-in. While these are among the most common and widely used authentication methods, offering more options—such as social media logins or phone number authentication—could provide greater flexibility for users to create accounts.
            - Customization for Specialized Needs: Although the app is functional for general blood donation drives, users with more specialized needs (e.g., large-scale events or specific site management configurations) may require additional customization. Expanding the app to allow for more customizable features could enhance its versatility and appeal for different use cases.
        Overall, while the application is a solid project for real-world usage, there is room for improvement in terms of authentication flexibility and user customization to meet diverse needs.
