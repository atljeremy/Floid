# Floid

Kotlin Flow Based Reactive Architecture.

# Install

### Gradle
`implementation "com.github.atljeremy:floid:0.0.1"`

# Usage

### Android View Model

Start by extending either `StatefulViewModel` (a.k.a `ViewModel`) or `AndroidStatefulViewModel` (a.k.a `AndroidVideoModel`). You'll need to provide an implementation for abstract functions `reducer(): Reducer` and `bind(flow:): Flow`. Here's an example of a basic view model with only a single action, in this case `DriveAction.Load`:
```kotlin
sealed class DriveState {
    data class Initial(val drive: Drive? = null): DriveState()
    object Loading: DriveState()
    data class LoadSuccess(val page: Int, val nextPage: Int?, val value: Drive?): DriveState()
    data class LoadFailed(val error: String? = null): DriveState()
}

sealed class DriveAction {
    data class Load(val user: User, val page: Int = 1): DriveAction()
}

private const val LinkKey = "Link"

@FlowPreview
@ExperimentalCoroutinesApi
class MapDriveViewModel(application: Application): AndroidStatefulViewModel<DriveAction, Response<Drive>, DriveState>(application) {

    @Inject
    lateinit var repository: DriveRepository

    override fun reducer() = object : Reducer<DriveAction, Response<Drive>, DriveState> {
        override val initialState: DriveState
            get() = DriveState.Initial()

        override suspend fun perform(action: DriveAction): Flow<Output<DriveAction, Response<Drive>>> =
                flow {
                    when (action) {
                        is DriveAction.Load -> emit(Output(action, repository.loadDrives(action.user, action.page)))
                    }
                }

        override suspend fun reduce(action: DriveAction, state: DriveState, data: Response<Drive>?) =
                when(action) {
                    is DriveAction.Load -> {
                        data?.let {
                            val linkAttributes = it.headers().get(LinkKey)?.parseLinkNextAttributes()
                            val nextPage = linkAttributes?.get("page")
                            DriveState.LoadSuccess(action.page, nextPage, it.body())
                        } ?: DriveState.LoadFailed(null)
                    }
                }
    }

    init {
        MapDriveDISingleton.getComponent(getApplication()).inject(this)
    }

    override fun bind(flow: Flow<DriveState>): Flow<DriveState> =
            flow
                .onStart { emit(DriveState.Loading) }
                .catch { emit(DriveState.LoadFailed(error = it.localizedMessage)) }

}
```

And this is the corresponding `Fragment`:
```kotlin
class MapDriveFragment : Fragment(), OnMapReadyCallback {
    private lateinit var viewModel: MapDriveViewModel
    private var map: GoogleMap? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this).get(MapDriveViewModel::class.java)
        // `viewModel.state` is a [LiveData] object with the type constraint you configure in the view model.
        // In this case, the type constraint is [DriveState] (a.k.a LiveData<DriveState>).
        // Any emission from this [LiveData] object will call `render` with the emitted state.
        viewModel.state.observe(viewLifecycleOwner, ::render)
        
        initializeMapFragment()
        //...
    }

    /**
     * Receives all state emissions produced from the view model that pass through the reducer.
     */
    private fun render(state: DriveState) {
        when(state) {
            is DriveState.Initial -> Unit
            is DriveState.Loading -> renderLoadingUI()
            is DriveState.LoadSuccess -> {
                updatePoints(state)
                state.nextPage?.let {
                    viewModel.dispatch(DriveAction.Load(user, it))
                }
            }
            is DriveState.LoadFailed -> renderLoadFailedUI(state.error)
        }
    }

    private fun initializeMapFragment() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapDriveView) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap
        val map = map ?: return
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isZoomControlsEnabled = false
        map.setPadding(0, 140, 0, 0)
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        // Call `dispatch` and pass in the action type you configured in the view model.
        // In this case, that's `DriveAction`.
        viewModel.dispatch(DriveAction.Load(user))
    }

    //...
}
```
