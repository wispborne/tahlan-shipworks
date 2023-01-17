package org.niatahl.tahlan.lostech

import org.niatahl.tahlan.questgiver.IInteractionLogic
import org.niatahl.tahlan.questgiver.InteractionDialogLogic
import org.niatahl.tahlan.questgiver.Questgiver.game
import org.niatahl.tahlan.questgiver.wispLib.findFirst

/**
 * Player is contacted by ALLMOTHER through one of the destroyed lostech ships.
 * Offers to grant player a software upgrade for salvaged gen2 lostech ships (+1 level for integrated AI officer)
 * in exchange for leaving the NX-A behind so she can retrieve it.
 * Cieve will disapprove of the decision if taken, but remain friendly, she got what she really wanted
 */
class AllmotherOfferDialog(
        mission: NXAHubMission = game.sector.intelManager.findFirst()!!
) : InteractionDialogLogic<AllmotherOfferDialog>(
        onInteractionStarted = null,
        people = { listOfNotNull(NXAHubMission.allmother) },
        pages = listOf(
                IInteractionLogic.Page(
                        id = 1,
                        onPageShown = {
                            para { game.text["nxa_stg1_pg1_para1"] }
                            para { game.text["nxa_stg1_pg1_para2"] }
                            para { game.text["nxa_stg1_pg1_para3"] }
                        },
                        options = listOf(
                                IInteractionLogic.Option(
                                        // accept
                                        text = { game.text["nxa_stg1_pg1_opt1"] },
                                        onOptionSelected = {
                                            para { game.text["nxa_stg1_pg1_opt1_onSelected"] }

                                            mission.setCurrentStage(NXAHubMission.Stage.ReturnToOrigin, dialog, null)
                                            navigator.promptToContinue(game.text["nxa_stg1_pg1_opt1_onSelected_continue"]) {
                                                it.close(doNotOfferAgain = true)
                                            }
                                        }
                                ),
                                IInteractionLogic.Option(
                                        // put the dialog text in a comment for reference, makes file easier to read
                                        text = { game.text["nxa_stg1_pg1_opt2"] },
                                        onOptionSelected = { navigator ->
                                            para { game.text["nxa_stg1_pg1_opt2_onSelected"] }
                                            navigator.refreshOptions()
                                        }
                                ),
                                IInteractionLogic.Option(
                                        // decline
                                        text = { game.text["nxa_stg1_pg1_opt3"] },
                                        onOptionSelected = { navigator ->
                                            para { game.text["nxa_stg1_pg1_opt3_onSelected"] }
                                            navigator.promptToContinue(game.text["continue"]) {
                                                navigator.close(doNotOfferAgain = true)
                                            }
                                        }
                                )
                        )
                )
        )
)