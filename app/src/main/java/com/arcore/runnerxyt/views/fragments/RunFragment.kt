package com.arcore.runnerxyt.views.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arcore.runnerxyt.R
import com.arcore.runnerxyt.adapter.RunAdapter
import com.arcore.runnerxyt.other.SortType
import com.arcore.runnerxyt.other.TrackingUtility
import com.arcore.runnerxyt.viewmodel.MainViewModel
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_run.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions


@AndroidEntryPoint
class RunFragment : Fragment(R.layout.fragment_run), EasyPermissions.PermissionCallbacks {

    private lateinit var alertDialog: AlertDialog
    lateinit var runAdapter: RunAdapter
    private val viewModel: MainViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.Q)
    private val permissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //viewModel = (activity as MainActivity).mainViewModel
        runAdapter = RunAdapter()

        setupRecyclerView()
        checkRunTimePermission()
        fab.setOnClickListener {
            findNavController().navigate(R.id.action_runFragment2_to_trackingFragment)
        }

        when (viewModel.sortType) {
            SortType.DATE -> spFilter.setSelection(0)
            SortType.RUNNING_TIME -> spFilter.setSelection(1)
            SortType.DISTANCE -> spFilter.setSelection(2)
            SortType.AVG_SPEED -> spFilter.setSelection(3)
            SortType.CALORIES_BURNED -> spFilter.setSelection(4)
        }
        viewModel.runs.observe(viewLifecycleOwner, Observer { runs ->
            runAdapter.submitList(runs)
        })

        spFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>?) {}

            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                pos: Int,
                id: Long
            ) {
                when (pos) {
                    0 -> viewModel.sortRuns(SortType.DATE)
                    1 -> viewModel.sortRuns(SortType.RUNNING_TIME)
                    2 -> viewModel.sortRuns(SortType.DISTANCE)
                    3 -> viewModel.sortRuns(SortType.AVG_SPEED)
                    4 -> viewModel.sortRuns(SortType.CALORIES_BURNED)
                }
            }
        }
    }

    /**
     * Handles swipe-to-delete
     */
    private val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.layoutPosition
            val run = runAdapter.differ.currentList[position]
            viewModel.deleteRun(run)
            Snackbar.make(requireView(), "Successfully deleted run", Snackbar.LENGTH_LONG).apply {
                setAction("Undo") {
                    viewModel.insertRun(run)
                }
                show()
            }
        }
    }

    private fun setupRecyclerView() = rvRuns.apply {
        adapter = runAdapter
        layoutManager = LinearLayoutManager(activity)
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(this)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkRunTimePermission() {
        if (context?.let { TrackingUtility.checkAndRequestPermissions(it, permissions) } == true) {
            return
        }
        showAlertDialogue()
        Dexter.withContext(context as Activity)
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    // check if all permissions are granted
                    if (report.areAllPermissionsGranted()) {
                        return
                    }
                    // check for permanent denial of any permission
                    if (report.isAnyPermissionPermanentlyDenied) {
                        showSettingsDialog()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest?>?,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).withErrorListener {
                Toast.makeText(
                    context,
                    "Error occurred! ",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .onSameThread()
            .check()


        /**   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val locationCPermission = context?.let {
        ContextCompat.checkSelfPermission(
        it,
        Manifest.permission.ACCESS_COARSE_LOCATION
        )
        }
        val locationPermission =
        context?.let {
        ContextCompat.checkSelfPermission(
        it,
        Manifest.permission.ACCESS_FINE_LOCATION
        )
        }
        val backGroundLocation =
        context?.let {
        ContextCompat.checkSelfPermission(
        it,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
        }
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (locationCPermission != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (backGroundLocation != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (listPermissionsNeeded.isNotEmpty()) {
        activity?.let {
        showAlertDialogue()
        ActivityCompat.requestPermissions(
        it,
        listPermissionsNeeded.toTypedArray(),
        Constants.REQUEST_CODE_LOCATION_PERMISSION
        )
        }
        return
        }
        return
        }
         **/
        /**    Dexter.withContext(context)
        .withPermissions(
        permissions
        ).withListener(object : MultiplePermissionsListener {
        override fun onPermissionsChecked(report: MultiplePermissionsReport) { /* ... */
        if (report.areAllPermissionsGranted()) {
        return
        }

        if (report.isAnyPermissionPermanentlyDenied) {
        checkRunTimePermission()
        }
        }

        override fun onPermissionRationaleShouldBeShown(
        permissions: List<PermissionRequest?>?,
        token: PermissionToken?
        ) {
        token?.continuePermissionRequest()
        }
        }).onSameThread().check()**/
        /**  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        if (ActivityCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        ) {
        activity?.let {
        ActivityCompat.requestPermissions(
        it, arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ),
        REQUEST_CODE_LOCATION_PERMISSION
        )
        }
        } else {
        Log.e("DB", "PERMISSION GRANTED")
        return
        }
        }**/
        /**  if (TrackingUtility.hasLocationPermissions(requireContext())) {
        return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        EasyPermissions.requestPermissions(
        this,
        "You need to accept location permission to use this app.",
        REQUEST_CODE_LOCATION_PERMISSION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        } else {
        EasyPermissions.requestPermissions(
        this,
        "You need to accept location permission to use this app.",
        REQUEST_CODE_LOCATION_PERMISSION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
        }**/
        /** if (TrackingUtility.hasLocationPermissions(requireContext())) {
        return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        EasyPermissions.requestPermissions(
        this,
        "You need to accept location permission to use this app",
        REQUEST_CODE_LOCATION_PERMISSION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
        )
        } else {
        EasyPermissions.requestPermissions(
        this,
        "You need to accept location permissions to use this app",
        REQUEST_CODE_LOCATION_PERMISSION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
        }
         **/

    }

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Need Permissions")
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.")
        builder.setPositiveButton(
            "GOTO SETTINGS"
        ) { dialog, which ->
            dialog.cancel()
            openSettings()
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, which -> dialog.cancel() }
        builder.show()
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", activity?.packageName, null)
        intent.data = uri
        startActivityForResult(intent, 101)
    }


    private fun showAlertDialogue() {

        alertDialog = activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setIcon(R.drawable.ic_baseline_warning_24)
                setTitle("Required Permission")
                setMessage(
                    "The app required user's permission to track the location in background " +
                            "and update the covered distance by user accordingly, " +
                            "to display then on the screen by calculating distance covered and speed."
                )
                setPositiveButton("Ok",
                    DialogInterface.OnClickListener { dialog, id ->
                        finish();
                    })
            }
            builder.create()
        }!!

    }

    private fun finish() {
        alertDialog.dismiss()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
    }

    /** @RequiresApi(Build.VERSION_CODES.Q)
    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
    if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
    AppSettingsDialog.Builder(this).build().show()
    } else {
    checkRunTimePermission()
    }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
    ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
     **/
}

